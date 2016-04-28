package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.*;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

import org.jetbrains.annotations.Nullable;
import org.junit.runner.Description;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.profiler.Profiler;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.*;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDERR;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;
import static org.testcontainers.utility.CommandLine.runShellCommand;

/**
 * Base class for that allows a container to be launched and controlled.
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class GenericContainer extends FailureDetectingExternalResource implements LinkableContainer {

    public static final int STARTUP_RETRY_COUNT = 3;

    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static final int CONTAINER_RUNNING_TIMEOUT_SEC = 30;


    /*
     * Default settings
     */
    @NonNull
    private List<Integer> exposedPorts = new ArrayList<>();

    @NonNull
    private List<String> portBindings = new ArrayList<>();

    @NonNull
    private List<String> extraHosts = new ArrayList<>();

    @NonNull
    private Future<String> image;

    @NonNull
    private List<String> env = new ArrayList<>();

    @Nullable
    private String[] commandParts = null;

    @NonNull
    private List<Bind> binds = new ArrayList<>();

    @NonNull
    private Map<String, LinkableContainer> linkedContainers = new HashMap<>();

    @NonNull
    private Duration startupTimeout = Duration.ofSeconds(60);

    @NonNull
    private Duration minimumRunningDuration = null;

    /*
     * Unique instance of DockerClient for use by this container object.
     */
    protected DockerClient dockerClient = DockerClientFactory.instance().client();

    /*
     * Info about the Docker server; lazily fetched.
     */
    protected Info dockerDaemonInfo = null;

    /*
     * Set during container startup
     */
    protected String containerId;
    protected String containerName;

    @Nullable
    private InspectContainerResponse containerInfo;


    private static final Set<String> AVAILABLE_IMAGE_NAME_CACHE = new HashSet<>();
    private static final RateLimiter DOCKER_CLIENT_RATE_LIMITER = RateLimiterBuilder
            .newBuilder()
            .withRate(1, TimeUnit.SECONDS)
            .withConstantThroughput()
            .build();

    public GenericContainer() {
        this("alpine:3.2");
    }

    public GenericContainer(@NonNull final String dockerImageName) {
        this.setDockerImageName(dockerImageName);
    }

    public GenericContainer(@NonNull final Future<String> image) {
        this.image = image;
    }

    /**
     * Starts the container using docker, pulling an image if necessary.
     */
    public void start() {
        int[] attempt = {0};
        Profiler profiler = new Profiler("Container startup");
        profiler.setLogger(logger());

        try {
            profiler.start("Prepare container configuration and host configuration");
            configure();

            logger().debug("Starting container: {}", getDockerImageName());

            Unreliables.retryUntilSuccess(STARTUP_RETRY_COUNT, () -> {
                attempt[0]++;
                logger().debug("Trying to start container: {} (attempt {}/{})", image.get(), attempt[0], STARTUP_RETRY_COUNT);
                this.tryStart(profiler.startNested("Container startup attempt #" + attempt[0]));
                return true;
            });
        } finally {
            profiler.stop().log();
        }
    }

    private void tryStart(Profiler profiler) {
        try {
            String dockerImageName = image.get();
            logger().debug("Starting container: {}", dockerImageName);

            logger().info("Creating container for image: {}", dockerImageName);
            profiler.start("Create container");
            CreateContainerCmd createCommand = dockerClient.createContainerCmd(dockerImageName);
            applyConfiguration(createCommand);
            containerId = createCommand.exec().getId();
            ContainerReaper.instance().registerContainerForCleanup(containerId, dockerImageName);

            logger().info("Starting container with ID: {}", containerId);
            profiler.start("Start container");
            dockerClient.startContainerCmd(containerId).exec();

            logger().info("Container {} is starting: {}", dockerImageName, containerId);

            // Tell subclasses that we're starting
            profiler.start("Inspecting container");
            containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
            containerName = containerInfo.getName();
            profiler.start("Call containerIsStarting on subclasses");
            containerIsStarting(containerInfo);

            // Wait until the container is running (may not be fully started)
            profiler.start("Wait until container state=running, or there's evidence it failed to start.");
            final Boolean[] startedOK = {null};
            Unreliables.retryUntilTrue(CONTAINER_RUNNING_TIMEOUT_SEC, TimeUnit.SECONDS, () -> {
                //noinspection CodeBlock2Expr
                return DOCKER_CLIENT_RATE_LIMITER.getWhenReady(() -> {
                    // record "now" before fetching status; otherwise the time to fetch the status
                    // will contribute to how long the container has been running.
                    Instant now = Instant.now();
                    InspectContainerResponse inspectionResponse = dockerClient.inspectContainerCmd(containerId).exec();

                    if (DockerStatus.isContainerRunning(
                            inspectionResponse.getState(),
                            minimumRunningDuration,
                            now)) {
                        startedOK[0] = true;
                        return true;
                    } else if (DockerStatus.isContainerStopped(inspectionResponse.getState())) {
                        startedOK[0] = false;
                        return true;
                    }
                    return false;
                });
            });

            if (!startedOK[0]) {

                logger().error("Container did not start correctly; container log output (if any) will be fetched and logged shortly");
                FrameConsumerResultCallback resultCallback = new FrameConsumerResultCallback();
                resultCallback.addConsumer(STDOUT, new Slf4jLogConsumer(logger()));
                resultCallback.addConsumer(STDERR, new Slf4jLogConsumer(logger()));
                dockerClient.logContainerCmd(containerId).withStdOut(true).withStdErr(true).exec(resultCallback);

                // Bail out, don't wait for the port to start listening.
                // (Exception thrown here will be caught below and wrapped)
                throw new IllegalStateException("Container has already stopped.");
            }

            profiler.start("Wait until container started");
            waitUntilContainerStarted();

            logger().info("Container {} started", dockerImageName);
            containerIsStarted(containerInfo);
        } catch (Exception e) {
            logger().error("Could not start container", e);

            throw new ContainerLaunchException("Could not create/start container", e);
        } finally {
            profiler.stop();
        }
    }

    /**
     * Stops the container.
     */
    public void stop() {

        if (containerId == null) {
            return;
        }

        String imageName;

        try {
            imageName = image.get();
        } catch (Exception e) {
            imageName = "<unknown>";
        }

        ContainerReaper.instance().stopAndRemoveContainer(containerId, imageName);
    }

    /**
     * Provide a logger that references the docker image name.
     *
     * @return a logger that references the docker image name
     */
    protected Logger logger() {
        return DockerLoggerFactory.getLogger(this.getDockerImageName());
    }

    /**
     * Creates a directory on the local filesystem which will be mounted as a volume for the container.
     *
     * @param temporary is the volume directory temporary? If true, the directory will be deleted on JVM shutdown.
     * @return path to the volume directory
     * @throws IOException
     */
    protected Path createVolumeDirectory(boolean temporary) throws IOException {
        Path directory = new File(".tmp-volume-" + System.currentTimeMillis()).toPath();
        PathOperations.mkdirp(directory);

        if (temporary) Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            PathOperations.recursiveDeleteDir(directory);
        }));

        return directory;
    }

    protected void configure() {

    }

    @SuppressWarnings({"EmptyMethod", "UnusedParameters"})
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
    }

    @SuppressWarnings({"EmptyMethod", "UnusedParameters"})
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
    }

    protected Integer getLivenessCheckPort() {
        if (exposedPorts.size() > 0) {
            return getMappedPort(exposedPorts.get(0));
        } else if (portBindings.size() > 0) {
            return PortBinding.parse(portBindings.get(0)).getBinding().getHostPort();
        } else {
            return null;
        }
    }

    private void applyConfiguration(CreateContainerCmd createCommand) {

        // Set up exposed ports (where there are no host port bindings defined)
        ExposedPort[] portArray = exposedPorts.stream()
                .map(ExposedPort::new)
                .toArray(ExposedPort[]::new);

        createCommand.withExposedPorts(portArray);

        // Set up exposed ports that need host port bindings
        PortBinding[] portBindingArray = portBindings.stream()
                .map(PortBinding::parse)
                .toArray(PortBinding[]::new);

        createCommand.withPortBindings(portBindingArray);

        if (commandParts != null) {
            createCommand.withCmd(commandParts);
        }

        String[] envArray = env.stream()
                .toArray(String[]::new);
        createCommand.withEnv(envArray);

        Bind[] bindsArray = binds.stream()
                .toArray(Bind[]::new);
        createCommand.withBinds(bindsArray);

        Link[] linksArray = linkedContainers.entrySet().stream()
                .map(entry -> new Link(entry.getValue().getContainerName(), entry.getKey()))
                .collect(Collectors.toList())
                .toArray(new Link[linkedContainers.size()]);
        createCommand.withLinks(linksArray);

        createCommand.withPublishAllPorts(true);

        String[] extraHostsArray = extraHosts.stream()
        		 .toArray(String[]::new);
        createCommand.withExtraHosts(extraHostsArray);
    }

    /**
     * Wait until the container has started. The default implementation simply
     * waits for a port to start listening; subclasses may override if more
     * sophisticated behaviour is required.
     */
    protected void waitUntilContainerStarted() {
        waitForListeningPort(DockerClientFactory.instance().dockerHostIpAddress(), getLivenessCheckPort());
    }

    /**
     * Waits for a port to start listening for incoming connections.
     *
     * @param ipAddress the IP address to attempt to connect to
     * @param port      the port which will start accepting connections
     */
    protected void waitForListeningPort(String ipAddress, Integer port) {

        if (port == null) {
            return;
        }

        try {
            Unreliables.retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                DOCKER_CLIENT_RATE_LIMITER.doWhenReady(() -> {
                    try {
                        new Socket(ipAddress, port).close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                return true;
            });
        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for container port to open (" + ipAddress + ":" + port + " should be listening)");
        }
    }


    /**
     * Set the command that should be run in the container
     *
     * @param command a command in single string format (will automatically be split on spaces)
     */
    public void setCommand(@NonNull String command) {
        this.commandParts = command.split(" ");
    }

    /**
     * Set the command that should be run in the container
     *
     * @param commandParts a command as an array of string parts
     */
    public void setCommand(@NonNull String... commandParts) {
        this.commandParts = commandParts;
    }

    /**
     * Add an environment variable to be passed to the container.
     *
     * @param key   environment variable key
     * @param value environment variable value
     */
    public void addEnv(String key, String value) {
        env.add(key + "=" + value);
    }

    /**
     * Adds a file system binding.
     *
     * @param hostPath the file system path on the host
     * @param containerPath the file system path inside the container
     * @param mode the bind mode
     */
    public void addFileSystemBind(String hostPath, String containerPath, BindMode mode) {
        binds.add(new Bind(hostPath, new Volume(containerPath), mode.accessMode));
    }

    /**
     * Adds a file system binding.
     *
     * @param hostPath the file system path on the host
     * @param containerPath the file system path inside the container
     * @param mode the bind mode
     * @return this
     */
    public GenericContainer withFileSystemBind(String hostPath, String containerPath, BindMode mode) {
        addFileSystemBind(hostPath, containerPath, mode);
        return this;
    }

    public void addLink(LinkableContainer otherContainer, String alias) {
        this.linkedContainers.put(alias, otherContainer);
    }

    public void addExposedPort(Integer port) {
        exposedPorts.add(port);
    }

    public void addExposedPorts(int... ports) {
        for (int port : ports) {
            exposedPorts.add(port);
        }
    }

    @Override
    protected void starting(Description description) {
        this.start();
    }

    @Override
    protected void finished(Description description) {
        this.stop();
    }


    /**
     * Set the ports that this container listens on
     *
     * @param ports an array of TCP ports
     * @return this
     */
    public GenericContainer withExposedPorts(Integer... ports) {
        this.setExposedPorts(asList(ports));
        return this;

    }

    /**
     * Add a container port that should be bound to a fixed port on the docker host.
     * <p>
     * Note that this method is protected scope to discourage use, as clashes or instability are more likely when
     * using fixed port mappings. If you need to use this method from a test, please use {@link FixedHostPortGenericContainer}
     * instead of GenericContainer.
     *
     * @param hostPort
     * @param containerPort
     */
    protected void addFixedExposedPort(int hostPort, int containerPort) {
        portBindings.add(String.format("%d:%d", hostPort, containerPort));
    }

    /**
     * Add an environment variable to be passed to the container.
     *
     * @param key   environment variable key
     * @param value environment variable value
     * @return this
     */
    public GenericContainer withEnv(String key, String value) {
        this.addEnv(key, value);
        return this;
    }

    /**
     * Set the command that should be run in the container
     *
     * @param cmd a command in single string format (will automatically be split on spaces)
     * @return this
     */
    public GenericContainer withCommand(String cmd) {
        this.setCommand(cmd);
        return this;
    }

    /**
     * Set the command that should be run in the container
     *
     * @param commandParts a command as an array of string parts
     * @return this
     */
    public GenericContainer withCommand(String... commandParts) {
        this.setCommand(commandParts);
        return this;
    }

    /**
     * Add an extra host entry to be passed to the container
     * @param hostname
     * @param ipAddress
     * @return this
     */
    public GenericContainer withExtraHost(String hostname, String ipAddress) {
        this.extraHosts.add(String.format("%s:%s", hostname, ipAddress));
        return this;
    }

    /**
     * Map a resource (file or directory) on the classpath to a path inside the container.
     * This will only work if you are running your tests outside a Docker container.
     *
     * @param resourcePath  path to the resource on the classpath (relative to the classpath root; should not start with a leading slash)
     * @param containerPath path this should be mapped to inside the container
     * @param mode          access mode for the file
     * @return this
     */
    public GenericContainer withClasspathResourceMapping(String resourcePath, String containerPath, BindMode mode) {
        URL resource = GenericContainer.class.getClassLoader().getResource(resourcePath);

        if (resource == null) {
            throw new IllegalArgumentException("Could not find classpath resource at provided path: " + resourcePath);
        }
        String resourceFilePath = resource.getFile();

        this.addFileSystemBind(resourceFilePath, containerPath, mode);

        return this;
    }

    /**
     * Set the duration of waiting time until container treated as started.
     * @see GenericContainer#waitForListeningPort(String, Integer)
     *
     * @param startupTimeout timeout
     * @return this
     */
    public GenericContainer withStartupTimeout(Duration startupTimeout) {
        this.setStartupTimeout(startupTimeout);
        return this;
    }

    /**
     * Get the IP address that this container may be reached on (may not be the local machine).
     *
     * @return an IP address
     */
    public String getContainerIpAddress() {
        return DockerClientFactory.instance().dockerHostIpAddress();
    }

    /**
     * Only consider a container to have successfully started if it has been running for this duration. The default
     * value is null; if that's the value, ignore this check.
     */

    public GenericContainer withMinimumRunningDuration(Duration minimumRunningDuration) {
        this.setMinimumRunningDuration(minimumRunningDuration);
        return this;
    }

    /**
     * Get the IP address that this container may be reached on (may not be the local machine).
     *
     * @return an IP address
     * @deprecated please use getContainerIpAddress() instead
     */
    @Deprecated
    public String getIpAddress() {
        return getContainerIpAddress();
    }

    /**
     * @return is the container currently running?
     */
    public Boolean isRunning() {
        try {
            return dockerClient.inspectContainerCmd(containerId).exec().getState().isRunning();
        } catch (DockerException e) {
            return false;
        }
    }

    /**
     * Get the actual mapped port for a given port exposed by the container.
     *
     * @param originalPort the original TCP port that is exposed
     * @return the port that the exposed port is mapped to, or null if it is not exposed
     */
    public Integer getMappedPort(final int originalPort) {

        Preconditions.checkState(containerId != null, "Mapped port can only be obtained after the container is started");

        Ports.Binding[] binding = new Ports.Binding[0];
        if (containerInfo != null) {
            binding = containerInfo.getNetworkSettings().getPorts().getBindings().get(new ExposedPort(originalPort));
        }

        if (binding != null && binding.length > 0 && binding[0] != null) {
            return binding[0].getHostPort();
        } else {
            throw new IllegalArgumentException("Requested port (" + originalPort +") is not mapped");
        }
    }

    /**
     * <b>Resolve</b> Docker image and set it.
     *
     * @param dockerImageName image name
     */
    public void setDockerImageName(@NonNull String dockerImageName) {
        this.image = new RemoteDockerImage(dockerImageName);

        // Mimic old behavior where we resolve image once it's set
        getDockerImageName();
    }

    /**
     * Get image name.
     *
     * @return image name
     */
    @NonNull
    public String getDockerImageName() {
        try {
            return image.get();
        } catch (Exception e) {
            throw new ContainerFetchException("Can't get Docker image name from " + image, e);
        }
    }

    /**
     * Get the IP address that containers (e.g. browsers) can use to reference a service running on the local machine,
     * i.e. the machine on which this test is running.
     * <p>
     * For example, if a web server is running on port 8080 on this local machine, the containerized web driver needs
     * to be pointed at "http://" + getTestHostIpAddress() + ":8080" in order to access it. Trying to hit localhost
     * from inside the container is not going to work, since the container has its own IP address.
     *
     * @return the IP address of the host machine
     */
    public String getTestHostIpAddress() {
        if (DockerMachineClient.instance().isInstalled()) {
            try {
                Optional<String> defaultMachine = DockerMachineClient.instance().getDefaultMachine();
                if (!defaultMachine.isPresent()) {
                    throw new IllegalStateException("Could not find a default docker-machine instance");
                }

                String sshConnectionString = runShellCommand("docker-machine", "ssh", defaultMachine.get(), "echo $SSH_CONNECTION").trim();
                if (Strings.isNullOrEmpty(sshConnectionString)) {
                    throw new IllegalStateException("Could not obtain SSH_CONNECTION environment variable for docker machine " + defaultMachine.get());
                }

                String[] sshConnectionParts = sshConnectionString.split("\\s");
                if (sshConnectionParts.length != 4) {
                    throw new IllegalStateException("Unexpected pattern for SSH_CONNECTION for docker machine - expected 'IP PORT IP PORT' pattern but found '" + sshConnectionString + "'");
                }

                return sshConnectionParts[0];
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {
            throw new UnsupportedOperationException("getTestHostIpAddress() is only implemented for docker-machine right now");
        }
    }

    /**
     * Follow container output, sending each frame (usually, line) to a consumer. Stdout and stderr will be followed.
     *
     * @param consumer consumer that the frames should be sent to
     */
    public void followOutput(Consumer<OutputFrame> consumer) {
        this.followOutput(consumer, OutputFrame.OutputType.STDOUT, OutputFrame.OutputType.STDERR);
    }

    /**
     * Follow container output, sending each frame (usually, line) to a consumer. This method allows Stdout and/or stderr
     * to be selected.
     *
     * @param consumer consumer that the frames should be sent to
     * @param types    types that should be followed (one or both of STDOUT, STDERR)
     */
    public void followOutput(Consumer<OutputFrame> consumer, OutputFrame.OutputType... types) {
        LogContainerCmd cmd = dockerClient.logContainerCmd(containerId)
                .withFollowStream(true);

        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        for (OutputFrame.OutputType type : types) {
            callback.addConsumer(type, consumer);
            if (type == STDOUT) cmd.withStdOut(true);
            if (type == STDERR) cmd.withStdErr(true);
        }

        cmd.exec(callback);
    }

    public synchronized Info fetchDockerDaemonInfo() throws IOException {

        if (this.dockerDaemonInfo == null) {
            this.dockerDaemonInfo = this.dockerClient.infoCmd().exec();
        }
        return this.dockerDaemonInfo;
    }

    /**
     * Run a command inside a running container, as though using "docker exec", and interpreting
     * the output as UTF8.
     * <p>
     * @see #execInContainer(Charset, String...)
     */
    public ExecResult execInContainer(String... command)
            throws UnsupportedOperationException, IOException, InterruptedException {

        return execInContainer(UTF8, command);
    }

    /**
     * Run a command inside a running container, as though using "docker exec".
     * <p>
     * This functionality is not available on a docker daemon running the older "lxc" execution driver. At
     * the time of writing, CircleCI was using this driver.
     * @param outputCharset the character set used to interpret the output.
     * @param command the parts of the command to run
     * @return the result of execution
     * @throws IOException if there's an issue communicating with Docker
     * @throws InterruptedException if the thread waiting for the response is interrupted
     * @throws UnsupportedOperationException if the docker daemon you're connecting to doesn't support "exec".
     */
    public ExecResult execInContainer(Charset outputCharset, String... command)
            throws UnsupportedOperationException, IOException, InterruptedException {

        if (fetchDockerDaemonInfo().getExecutionDriver().startsWith("lxc")) {
            // at time of writing, this is the expected result in CircleCI.
            throw new UnsupportedOperationException(
                "Your docker daemon is running the \"lxc\" driver, which doesn't support \"docker exec\".");
        }

        this.dockerClient
            .execCreateCmd(this.containerId)
            .withCmd(command);

        logger().info("Running \"exec\" command: " + String.join(" ", command));
        final ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(this.containerId)
            .withAttachStdout(true).withAttachStderr(true).withCmd(command).exec();

        final ToStringConsumer stdoutConsumer = new ToStringConsumer();
        final ToStringConsumer stderrConsumer = new ToStringConsumer();

        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        callback.addConsumer(OutputFrame.OutputType.STDOUT, stdoutConsumer);
        callback.addConsumer(OutputFrame.OutputType.STDERR, stderrConsumer);

        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(callback).awaitCompletion();

        final ExecResult result = new ExecResult(
              stdoutConsumer.toString(outputCharset),
              stderrConsumer.toString(outputCharset));

        logger().trace("stdout: " + result.getStdout());
        logger().trace("stderr: " + result.getStderr());
        return result;
    }

    /**
     * Class to hold results from a "docker exec" command. Note that, due to the limitations of the
     * docker API, there's no easy way to get the result code from the process we ran.
     */
    public static class ExecResult {
        private final String stdout;
        private final String stderr;

        public ExecResult(String stdout, String stderr) {
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }
    }
}

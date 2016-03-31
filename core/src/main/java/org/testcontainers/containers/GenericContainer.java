package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import com.github.dockerjava.core.command.PullImageResultCallback;
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
import org.slf4j.LoggerFactory;
import org.slf4j.profiler.Profiler;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.utility.ContainerReaper;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerMachineClient;
import org.testcontainers.utility.PathOperations;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
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
    /*
                 * Default settings
                 */
    @NonNull
    private List<Integer> exposedPorts = new ArrayList<>();

    @NonNull
    private List<String> portBindings = new ArrayList<>();

    @NonNull
    private String dockerImageName = "alpine:3.2";

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

    /*
     * Unique instance of DockerClient for use by this container object.
     */
    protected DockerClient dockerClient = DockerClientFactory.instance().client();

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

    }

    public GenericContainer(@NonNull final String dockerImageName) {
        this.setDockerImageName(dockerImageName);
    }

    /**
     * Starts the container using docker, pulling an image if necessary.
     */
    public void start() {
        int[] attempt = {0};
        Unreliables.retryUntilSuccess(STARTUP_RETRY_COUNT, () -> {
            attempt[0]++;
            logger().debug("Trying to start container: {} (attempt {}/{})", dockerImageName, attempt[0], STARTUP_RETRY_COUNT);
            this.tryStart();
            return true;
        });
    }

    private void tryStart() {
        Profiler profiler = new Profiler("Container startup");
        profiler.setLogger(logger());

        logger().debug("Starting container: {}", dockerImageName);

        try {

            pullImageIfNeeded(dockerImageName, profiler.startNested("Pull image if needed"));

            profiler.start("Prepare container configuration and host configuration");
            configure();

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
            profiler.start("Wait until container state=running");
            Unreliables.retryUntilTrue(30, TimeUnit.SECONDS, () -> {
                //noinspection CodeBlock2Expr
                return DOCKER_CLIENT_RATE_LIMITER.getWhenReady(() -> {
                    InspectContainerResponse inspectionResponse = dockerClient.inspectContainerCmd(containerId).exec();
                    return inspectionResponse.getState().isRunning();
                });
            });

            profiler.start("Wait until container started");
            waitUntilContainerStarted();

            logger().info("Container {} started", dockerImageName);
            containerIsStarted(containerInfo);

        } catch (Exception e) {
            logger().error("Could not start container", e);

            throw new ContainerLaunchException("Could not create/start container", e);
        } finally {
            profiler.stop().log();
        }
    }

    /**
     * Stops the container.
     */
    public void stop() {

        if (containerId == null) {
            return;
        }

        ContainerReaper.instance().stopAndRemoveContainer(containerId, dockerImageName);
    }

    /**
     * Provide a logger that references the docker image name.
     *
     * @return a logger that references the docker image name
     */
    protected Logger logger() {
        if ("UTF-8".equals(System.getProperty("file.encoding"))) {
            return LoggerFactory.getLogger("\uD83D\uDC33 [" + dockerImageName + "]");
        } else {
            return LoggerFactory.getLogger("docker[" + dockerImageName + "]");
        }
    }

    private void pullImageIfNeeded(final String imageName, Profiler profiler) throws DockerException, InterruptedException {

        // Does our cache already know the image?
        if (AVAILABLE_IMAGE_NAME_CACHE.contains(imageName)) {
            return;
        }

        profiler.start("Check local images");
        // Update the cache
        List<Image> images = dockerClient.listImagesCmd().exec();
        for (Image image : images) {
            Collections.addAll(AVAILABLE_IMAGE_NAME_CACHE, image.getRepoTags());
        }

        // Check cache again following update
        if (AVAILABLE_IMAGE_NAME_CACHE.contains(imageName)) {
            return;
        }

        logger().info("Pulling docker image: {}. Please be patient; this may take some time but only needs to be done once.", imageName);
        profiler.start("Pull image");
        int attempts = 0;
        while (!AVAILABLE_IMAGE_NAME_CACHE.contains(imageName)) {
            // The image is not available locally - pull it

            if (attempts++ >= 3) {
                logger().error("Retry limit reached while trying to pull image: " + imageName + ". Please check output of `docker pull " + imageName + "`");
                throw new ContainerFetchException("Retry limit reached while trying to pull image: " + imageName);
            }

            PullImageResultCallback resultCallback = dockerClient.pullImageCmd(imageName).exec(new PullImageResultCallback());

            resultCallback.awaitCompletion();

            // Confirm successful pull, otherwise may need to retry...
            // see https://github.com/docker/docker/issues/10708
            List<Image> updatedImages = dockerClient.listImagesCmd().exec();
            for (Image image : updatedImages) {
                Collections.addAll(AVAILABLE_IMAGE_NAME_CACHE, image.getRepoTags());
            }
        }
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

    public void addFileSystemBind(String hostPath, String containerPath, BindMode mode) {

        binds.add(new Bind(hostPath, new Volume(containerPath), mode.accessMode));
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
     * Map a resource (file or directory) on the classpath to a path inside the container
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

    public void setDockerImageName(@NonNull String dockerImageName) {
        DockerImageName.validate(dockerImageName);
        this.dockerImageName = dockerImageName;

        Profiler profiler = new Profiler("Rule creation - prefetch image");
        profiler.setLogger(logger());
        try {
            pullImageIfNeeded(dockerImageName, profiler);
        } catch (InterruptedException e) {
            throw new ContainerFetchException("Failed to fetch container image for " + dockerImageName, e);
        } finally {
            profiler.stop().log();
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

        for (OutputFrame.OutputType type : types) {
            if (type == STDOUT) cmd.withStdOut(true);
            if (type == STDERR) cmd.withStdErr(true);
        }

        cmd.exec(new ResultCallbackTemplate<ResultCallback<Frame>, Frame>() {
            @Override
            public void onNext(Frame dockerFrame) {
                OutputFrame.OutputType type;
                switch (dockerFrame.getStreamType()) {
                    case STDOUT:
                        type = STDOUT;
                        break;
                    case STDERR:
                        type = STDERR;
                        break;
                    default:
                        return;
                }

                OutputFrame frame = new OutputFrame(type, dockerFrame.getPayload());

                consumer.accept(frame);
            }

            @Override
            public void close() throws IOException {
                consumer.accept(OutputFrame.END);
                super.close();
            }
        });
    }
}

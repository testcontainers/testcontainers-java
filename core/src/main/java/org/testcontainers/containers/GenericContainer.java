package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.*;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.Nullable;
import org.junit.runner.Description;
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
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.wait.Wait;
import org.testcontainers.containers.wait.WaitStrategy;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
public class GenericContainer<SELF extends GenericContainer<SELF>>
        extends FailureDetectingExternalResource
        implements Container<SELF> {

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
    private String networkMode;

    @NonNull
    private Future<String> image;

    @NonNull
    private List<String> env = new ArrayList<>();

    @NonNull
    private String[] commandParts = new String[0];

    @NonNull
    private List<Bind> binds = new ArrayList<>();

    @NonNull
    private Map<String, LinkableContainer> linkedContainers = new HashMap<>();

    private StartupCheckStrategy startupCheckStrategy = new IsRunningStartupCheckStrategy();

    private int startupAttempts = 1;

    @Nullable
    private String workingDirectory = null;

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

    /**
     * The approach to determine if the container is ready.
     */
    @NonNull
    protected WaitStrategy waitStrategy = Wait.defaultWaitStrategy();

    @Nullable
    private InspectContainerResponse containerInfo;

    private List<Consumer<OutputFrame>> logConsumers = new ArrayList<>();

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
        Profiler profiler = new Profiler("Container startup");
        profiler.setLogger(logger());

        try {
            profiler.start("Prepare container configuration and host configuration");
            configure();

            logger().debug("Starting container: {}", getDockerImageName());
            logger().debug("Trying to start container: {}", image.get());

            AtomicInteger attempt = new AtomicInteger(0);
            Unreliables.retryUntilSuccess(startupAttempts, () -> {
                logger().debug("Trying to start container: {} (attempt {}/{})", image.get(), attempt.incrementAndGet(), startupAttempts);
                tryStart(profiler.startNested("Container startup attempt"));
                return true;
            });

        } catch (Exception e) {
            throw new ContainerLaunchException("Container startup failed", e);
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
            ResourceReaper.instance().registerContainerForCleanup(containerId, dockerImageName);

            logger().info("Starting container with ID: {}", containerId);
            profiler.start("Start container");
            dockerClient.startContainerCmd(containerId).exec();

            // For all registered output consumers, start following as close to container startup as possible
            this.logConsumers.forEach(this::followOutput);

            logger().info("Container {} is starting: {}", dockerImageName, containerId);

            // Tell subclasses that we're starting
            profiler.start("Inspecting container");
            containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
            containerName = containerInfo.getName();
            profiler.start("Call containerIsStarting on subclasses");
            containerIsStarting(containerInfo);

            // Wait until the container is running (may not be fully started)
            profiler.start("Wait until container has started properly, or there's evidence it failed to start.");

            if (!this.startupCheckStrategy.waitUntilStartupSuccessful(dockerClient, containerId)) {
                // Bail out, don't wait for the port to start listening.
                // (Exception thrown here will be caught below and wrapped)
                throw new IllegalStateException("Container did not start correctly.");
            }

            profiler.start("Wait until container started properly");
            waitUntilContainerStarted();

            logger().info("Container {} started", dockerImageName);
            containerIsStarted(containerInfo);
        } catch (Exception e) {
            logger().error("Could not start container", e);

            // Log output if startup failed, either due to a container failure or exception (including timeout)
            logger().error("Container log output (if any) will follow:");
            FrameConsumerResultCallback resultCallback = new FrameConsumerResultCallback();
            resultCallback.addConsumer(STDOUT, new Slf4jLogConsumer(logger()));
            resultCallback.addConsumer(STDERR, new Slf4jLogConsumer(logger()));
            dockerClient.logContainerCmd(containerId).withStdOut(true).withStdErr(true).exec(resultCallback);

            // Try to ensure that container log output is shown before proceeding
            try {
                resultCallback.getCompletionLatch().await(1, TimeUnit.MINUTES);
            } catch (InterruptedException ignored) {
                // Cannot do anything at this point
            }

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

        ResourceReaper.instance().stopAndRemoveContainer(containerId, imageName);
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
     */
    protected Path createVolumeDirectory(boolean temporary) {
        Path directory = new File(".tmp-volume-" + System.currentTimeMillis()).toPath();
        PathUtils.mkdirp(directory);

        if (temporary) Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            PathUtils.recursiveDeleteDir(directory);
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

    /**
     * @return the port on which to check if the container is ready
     */
    protected Integer getLivenessCheckPort() {
        if (exposedPorts.size() > 0) {
            return getMappedPort(exposedPorts.get(0));
        } else if (portBindings.size() > 0) {
            return Integer.valueOf(PortBinding.parse(portBindings.get(0)).getBinding().getHostPortSpec());
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

        Set<Link> allLinks = new HashSet<>();
        Set<String> allLinkedContainerNetworks = new HashSet<>();
        for (Map.Entry<String, LinkableContainer> linkEntries : linkedContainers.entrySet()) {

            String alias = linkEntries.getKey();
            LinkableContainer linkableContainer = linkEntries.getValue();

            Set<Link> links = dockerClient.listContainersCmd().exec().stream()
                    .filter(container -> container.getNames()[0].endsWith(linkableContainer.getContainerName()))
                    .map(container -> new Link(container.getNames()[0], alias))
                    .collect(Collectors.toSet());
            allLinks.addAll(links);

            boolean linkableContainerIsRunning = dockerClient.listContainersCmd().exec().stream()
                    .filter(container -> container.getNames()[0].endsWith(linkableContainer.getContainerName()))
                    .map(com.github.dockerjava.api.model.Container::getId)
                    .map(id -> dockerClient.inspectContainerCmd(id).exec())
                    .anyMatch(linkableContainerInspectResponse -> linkableContainerInspectResponse.getState().getRunning());

            if (!linkableContainerIsRunning) {
                throw new ContainerLaunchException("Aborting attempt to link to container " +
                        linkableContainer.getContainerName() +
                        " as it is not running");
            }

            Set<String> linkedContainerNetworks = dockerClient.listContainersCmd().exec().stream()
                    .filter(container -> container.getNames()[0].endsWith(linkableContainer.getContainerName()))
                    .filter(container -> container.getNetworkSettings() != null &&
                            container.getNetworkSettings().getNetworks() != null)
                    .flatMap(container -> container.getNetworkSettings().getNetworks().keySet().stream())
                    .distinct()
                    .collect(Collectors.toSet());
            allLinkedContainerNetworks.addAll(linkedContainerNetworks);
        }

        createCommand.withLinks(allLinks.toArray(new Link[allLinks.size()]));

        allLinkedContainerNetworks.remove("bridge");
        if (allLinkedContainerNetworks.size() > 1) {
            logger().warn("Container needs to be on more than one custom network to link to other " +
                            "containers - this is not currently supported. Required networks are: {}",
                    allLinkedContainerNetworks);
        }

        Optional<String> networkForLinks = allLinkedContainerNetworks.stream().findFirst();
        if (networkForLinks.isPresent()) {
            logger().debug("Associating container with network: {}", networkForLinks.get());
            createCommand.withNetworkMode(networkForLinks.get());
        }

        createCommand.withPublishAllPorts(true);

        String[] extraHostsArray = extraHosts.stream()
                .toArray(String[]::new);
        createCommand.withExtraHosts(extraHostsArray);

        if (networkMode != null) {
            createCommand.withNetworkMode(networkMode);
        }

        if (workingDirectory != null) {
            createCommand.withWorkingDir(workingDirectory);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF waitingFor(@NonNull WaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
        return self();
    }

    /**
     * The {@link WaitStrategy} to use to determine if the container is ready.
     * Defaults to {@link Wait#defaultWaitStrategy()}.
     *
     * @return the {@link WaitStrategy} to use
     */
    protected WaitStrategy getWaitStrategy() {
        return waitStrategy;
    }

    /**
     * Wait until the container has started. The default implementation simply
     * waits for a port to start listening; other implementations are available
     * as implementations of {@link WaitStrategy}
     *
     * @see #waitingFor(WaitStrategy)
     */
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCommand(@NonNull String command) {
        this.commandParts = command.split(" ");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setCommand(@NonNull String... commandParts) {
        this.commandParts = commandParts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEnv(String key, String value) {
        env.add(key + "=" + value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFileSystemBind(String hostPath, String containerPath, BindMode mode) {
        if (SystemUtils.IS_OS_WINDOWS) {
            hostPath = PathUtils.createMinGWPath(hostPath);
        }

        binds.add(new Bind(hostPath, new Volume(containerPath), mode.accessMode));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withFileSystemBind(String hostPath, String containerPath, BindMode mode) {
        addFileSystemBind(hostPath, containerPath, mode);
        return self();
    }

    @Override
    public void addLink(LinkableContainer otherContainer, String alias) {
        this.linkedContainers.put(alias, otherContainer);
    }

    @Override
    public void addExposedPort(Integer port) {
        exposedPorts.add(port);
    }

    @Override
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
     * {@inheritDoc}
     */
    @Override
    public SELF withExposedPorts(Integer... ports) {
        this.setExposedPorts(asList(ports));
        return self();

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
     * {@inheritDoc}
     */
    @Override
    public SELF withEnv(String key, String value) {
        this.addEnv(key, value);
        return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withEnv(Map<String, String> env) {
        env.forEach(this::addEnv);
        return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withCommand(String cmd) {
        this.setCommand(cmd);
        return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withCommand(String... commandParts) {
        this.setCommand(commandParts);
        return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withExtraHost(String hostname, String ipAddress) {
        this.extraHosts.add(String.format("%s:%s", hostname, ipAddress));
        return self();
    }

    @Override
    public SELF withNetworkMode(String networkMode) {
        this.networkMode = networkMode;
        return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withClasspathResourceMapping(String resourcePath, String containerPath, BindMode mode) {
        URL resource = GenericContainer.class.getClassLoader().getResource(resourcePath);

        if (resource == null) {
            throw new IllegalArgumentException("Could not find classpath resource at provided path: " + resourcePath);
        }
        String resourceFilePath = resource.getFile();

        this.addFileSystemBind(resourceFilePath, containerPath, mode);

        return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withStartupTimeout(Duration startupTimeout) {
        getWaitStrategy().withStartupTimeout(startupTimeout);
        return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContainerIpAddress() {
        return DockerClientFactory.instance().dockerHostIpAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withMinimumRunningDuration(Duration minimumRunningDuration) {
        this.startupCheckStrategy = new MinimumDurationRunningStartupCheckStrategy(minimumRunningDuration);
        return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withStartupCheckStrategy(StartupCheckStrategy strategy) {
        this.startupCheckStrategy = strategy;
        return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withWorkingDirectory(String workDir) {
        this.setWorkingDirectory(workDir);
        return self();
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
     * {@inheritDoc}
     */
    @Override
    public Boolean isRunning() {
        try {
            return dockerClient.inspectContainerCmd(containerId).exec().getState().getRunning();
        } catch (DockerException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getMappedPort(final int originalPort) {

        Preconditions.checkState(containerId != null, "Mapped port can only be obtained after the container is started");

        Ports.Binding[] binding = new Ports.Binding[0];
        if (containerInfo != null) {
            binding = containerInfo.getNetworkSettings().getPorts().getBindings().get(new ExposedPort(originalPort));
        }

        if (binding != null && binding.length > 0 && binding[0] != null) {
            return Integer.valueOf(binding[0].getHostPortSpec());
        } else {
            throw new IllegalArgumentException("Requested port (" + originalPort + ") is not mapped");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDockerImageName(@NonNull String dockerImageName) {
        this.image = new RemoteDockerImage(dockerImageName);

        // Mimic old behavior where we resolve image once it's set
        getDockerImageName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @NonNull
    public String getDockerImageName() {
        try {
            return image.get();
        } catch (Exception e) {
            throw new ContainerFetchException("Can't get Docker image name from " + image, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
     * {@inheritDoc}
     */
    @Override
    public void followOutput(Consumer<OutputFrame> consumer) {
        this.followOutput(consumer, OutputFrame.OutputType.STDOUT, OutputFrame.OutputType.STDERR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withLogConsumer(Consumer<OutputFrame> consumer) {
        this.logConsumers.add(consumer);

        return self();
    }

    @Override
    public synchronized Info fetchDockerDaemonInfo() throws IOException {

        if (this.dockerDaemonInfo == null) {
            this.dockerDaemonInfo = this.dockerClient.infoCmd().exec();
        }
        return this.dockerDaemonInfo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecResult execInContainer(String... command)
            throws UnsupportedOperationException, IOException, InterruptedException {

        return execInContainer(UTF8, command);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecResult execInContainer(Charset outputCharset, String... command)
            throws UnsupportedOperationException, IOException, InterruptedException {

        if (!TestEnvironment.dockerExecutionDriverSupportsExec()) {
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
     * Allow container startup to be attempted more than once if an error occurs. To be if containers are
     * 'flaky' but this flakiness is not something that should affect test outcomes.
     *
     * @param attempts number of attempts
     */
    public void withStartupAttempts(int attempts) {
        this.startupAttempts = attempts;
    }

    /**
     * Convenience class with access to non-public members of GenericContainer.
     */
    public static abstract class AbstractWaitStrategy implements WaitStrategy {
        protected GenericContainer container;

        @NonNull
        protected Duration startupTimeout = Duration.ofSeconds(60);

        /**
         * Wait until the container has started.
         *
         * @param container the container for which to wait
         */
        @Override
        public void waitUntilReady(GenericContainer container) {
            this.container = container;
            waitUntilReady();
        }

        /**
         * Wait until {@link #container} has started.
         */
        protected abstract void waitUntilReady();

        /**
         * Set the duration of waiting time until container treated as started.
         *
         * @param startupTimeout timeout
         * @return this
         * @see WaitStrategy#waitUntilReady(GenericContainer)
         */
        public WaitStrategy withStartupTimeout(Duration startupTimeout) {
            this.startupTimeout = startupTimeout;
            return this;
        }

        /**
         * @return the container's logger
         */
        protected Logger logger() {
            return container.logger();
        }

        /**
         * @return the port on which to check if the container is ready
         */
        protected Integer getLivenessCheckPort() {
            return container.getLivenessCheckPort();
        }

        /**
         * @return the rate limiter to use
         */
        protected RateLimiter getRateLimiter() {
            return DOCKER_CLIENT_RATE_LIMITER;
        }
    }
}

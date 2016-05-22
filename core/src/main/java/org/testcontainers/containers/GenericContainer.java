package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.DockerException;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

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
import org.testcontainers.containers.traits.*;
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
public class GenericContainer<SELF extends GenericContainer<SELF>>
        extends FailureDetectingExternalResource
        implements Container<SELF> {

    public static final int STARTUP_RETRY_COUNT = 3;

    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static final int CONTAINER_RUNNING_TIMEOUT_SEC = 30;

    @NonNull
    private Future<String> image;

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

    /**
     * The approach to determine if the container is ready.
     */
    @NonNull
    protected WaitStrategy waitStrategy = Wait.defaultWaitStrategy();

    @Nullable
    private InspectContainerResponse containerInfo;

    private final List<Trait<SELF>> traits = new ArrayList<>();

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

    /**
     * @return the port on which to check if the container is ready
     */
    protected Integer getLivenessCheckPort() {
        return getExposedPorts().stream()
                .findFirst()
                .map(this::getMappedPort)
                .orElseGet(
                        () -> getPortBindings().stream()
                                .findFirst()
                                .map(binding -> PortBinding.parse(binding).getBinding().getHostPort())
                                .orElse(null)
                );
    }

    private void applyConfiguration(CreateContainerCmd createCommand) {
        createCommand.withPublishAllPorts(true);

        getTraits().stream().sorted(TRAIT_COMPARATOR).forEach(trait -> trait.configure(self(), createCommand));
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

    @Override
    protected void starting(Description description) {
        this.start();
    }

    @Override
    protected void finished(Description description) {
        this.stop();
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
        with(new org.testcontainers.containers.traits.PortBinding<>(hostPort, containerPort));
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
        this.setMinimumRunningDuration(minimumRunningDuration);
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
            return dockerClient.inspectContainerCmd(containerId).exec().getState().isRunning();
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
            return binding[0].getHostPort();
        } else {
            throw new IllegalArgumentException("Requested port (" + originalPort +") is not mapped");
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

package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.model.VolumesFrom;
import com.google.common.base.Strings;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Setter;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.jetbrains.annotations.NotNull;
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
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.wait.Wait;
import org.testcontainers.containers.wait.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
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
        implements Container<SELF>, AutoCloseable, WaitStrategyTarget {

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
    private Network network;

    @NonNull
    private List<String> networkAliases = new ArrayList<>(Arrays.asList(
            "tc-" + Base58.randomString(8)
    ));

    @NonNull
    private Future<String> image;

    @NonNull
    private Map<String, String> env = new HashMap<>();

    @NonNull
    private String[] commandParts = new String[0];

    @NonNull
    private List<Bind> binds = new ArrayList<>();

    private boolean privilegedMode;

    @NonNull
    private List<VolumesFrom> volumesFroms = new ArrayList<>();

    /**
     * @deprecated Links are deprecated (see <a href="https://github.com/testcontainers/testcontainers-java/issues/465">#465</a>). Please use {@link Network} features instead.
     */
    @NonNull
    @Deprecated
    private Map<String, LinkableContainer> linkedContainers = new HashMap<>();

    private StartupCheckStrategy startupCheckStrategy = new IsRunningStartupCheckStrategy();

    private int startupAttempts = 1;

    @Nullable
    private String workingDirectory = null;

    /*
     * Unique instance of DockerClient for use by this container object.
     */
    @Setter(AccessLevel.NONE)
    protected DockerClient dockerClient = DockerClientFactory.instance().client();

    /*
     * Info about the Docker server; lazily fetched.
     */
    @Setter(AccessLevel.NONE)
    protected Info dockerDaemonInfo = null;

    /*
     * Set during container startup
     */
    @Setter(AccessLevel.NONE)
    protected String containerId;

    @Setter(AccessLevel.NONE)
    protected String containerName;

    @Setter(AccessLevel.NONE)
    private InspectContainerResponse containerInfo;

    /**
     * The approach to determine if the container is ready.
     */
    @NonNull
    protected org.testcontainers.containers.wait.strategy.WaitStrategy waitStrategy = Wait.defaultWaitStrategy();

    private List<Consumer<OutputFrame>> logConsumers = new ArrayList<>();

    private final Set<Consumer<CreateContainerCmd>> createContainerCmdModifiers = new LinkedHashSet<>();

    private static final Set<String> AVAILABLE_IMAGE_NAME_CACHE = new HashSet<>();
    private static final RateLimiter DOCKER_CLIENT_RATE_LIMITER = RateLimiterBuilder
            .newBuilder()
            .withRate(1, TimeUnit.SECONDS)
            .withConstantThroughput()
            .build();


    public GenericContainer() {
        this(TestcontainersConfiguration.getInstance().getTinyImage());
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

        if (temporary) Runtime.getRuntime().addShutdownHook(new Thread(DockerClientFactory.TESTCONTAINERS_THREAD_GROUP, () -> {
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
     * @deprecated see {@link GenericContainer#getLivenessCheckPorts()} for replacement
     */
    @Deprecated
    protected Integer getLivenessCheckPort() {
        // legacy implementation for backwards compatibility
        if (exposedPorts.size() > 0) {
            return getMappedPort(exposedPorts.get(0));
        } else if (portBindings.size() > 0) {
            return Integer.valueOf(PortBinding.parse(portBindings.get(0)).getBinding().getHostPortSpec());
        } else {
            return null;
        }
    }

    /**
     * @return the ports on which to check if the container is ready
     * @deprecated use {@link #getLivenessCheckPortNumbers()} instead
     */
    @NotNull
    @NonNull
    @Deprecated
    protected Set<Integer> getLivenessCheckPorts() {
        final Set<Integer> result = WaitStrategyTarget.super.getLivenessCheckPortNumbers();

        // for backwards compatibility
        if (this.getLivenessCheckPort() != null) {
            result.add(this.getLivenessCheckPort());
        }

        return result;
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return this.getLivenessCheckPorts();
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

        String[] envArray = env.entrySet().stream()
                .map(it -> it.getKey() + "=" + it.getValue())
                .toArray(String[]::new);
        createCommand.withEnv(envArray);

        Bind[] bindsArray = binds.stream()
                .toArray(Bind[]::new);
        createCommand.withBinds(bindsArray);

        VolumesFrom[] volumesFromsArray = volumesFroms.stream()
                .toArray(VolumesFrom[]::new);
        createCommand.withVolumesFrom(volumesFromsArray);

        Set<Link> allLinks = new HashSet<>();
        Set<String> allLinkedContainerNetworks = new HashSet<>();
        for (Map.Entry<String, LinkableContainer> linkEntries : linkedContainers.entrySet()) {

            String alias = linkEntries.getKey();
            LinkableContainer linkableContainer = linkEntries.getValue();

            Set<Link> links = findLinksFromThisContainer(alias, linkableContainer);
            allLinks.addAll(links);

            if (allLinks.size() == 0) {
                throw new ContainerLaunchException("Aborting attempt to link to container " +
                        linkableContainer.getContainerName() +
                        " as it is not running");
            }

            Set<String> linkedContainerNetworks = findAllNetworksForLinkedContainers(linkableContainer);
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

        if (network != null) {
            createCommand.withNetworkMode(network.getId());
            createCommand.withAliases(this.networkAliases);
        } else if (networkMode != null) {
            createCommand.withNetworkMode(networkMode);
        }

        if (workingDirectory != null) {
            createCommand.withWorkingDir(workingDirectory);
        }

        if (privilegedMode) {
            createCommand.withPrivileged(privilegedMode);
        }

        createContainerCmdModifiers.forEach(hook -> hook.accept(createCommand));

        Map<String, String> labels = createCommand.getLabels();
        labels = new HashMap<>(labels != null ? labels : Collections.emptyMap());
        labels.putAll(DockerClientFactory.DEFAULT_LABELS);
        createCommand.withLabels(labels);
    }

    private Set<Link> findLinksFromThisContainer(String alias, LinkableContainer linkableContainer) {
        return dockerClient.listContainersCmd()
                .withStatusFilter(Arrays.asList("running"))
                .exec().stream()
                .flatMap(container -> Stream.of(container.getNames()))
                .filter(name -> name.endsWith(linkableContainer.getContainerName()))
                .map(name -> new Link(name, alias))
                .collect(Collectors.toSet());
    }

    private Set<String> findAllNetworksForLinkedContainers(LinkableContainer linkableContainer) {
        return dockerClient.listContainersCmd().exec().stream()
                .filter(container -> container.getNames()[0].endsWith(linkableContainer.getContainerName()))
                .filter(container -> container.getNetworkSettings() != null &&
                        container.getNetworkSettings().getNetworks() != null)
                .flatMap(container -> container.getNetworkSettings().getNetworks().keySet().stream())
                .distinct()
                .collect(Collectors.toSet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF waitingFor(@NonNull org.testcontainers.containers.wait.strategy.WaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
        return self();
    }

    /**
     * The {@link WaitStrategy} to use to determine if the container is ready.
     * Defaults to {@link Wait#defaultWaitStrategy()}.
     *
     * @return the {@link WaitStrategy} to use
     */
    protected org.testcontainers.containers.wait.strategy.WaitStrategy getWaitStrategy() {
        return waitStrategy;
    }

    @Override
    public void setWaitStrategy(org.testcontainers.containers.wait.strategy.WaitStrategy waitStrategy) {
        this.waitStrategy = waitStrategy;
    }

    /**
     * Wait until the container has started. The default implementation simply
     * waits for a port to start listening; other implementations are available
     * as implementations of {@link org.testcontainers.containers.wait.strategy.WaitStrategy}
     *
     * @see #waitingFor(org.testcontainers.containers.wait.strategy.WaitStrategy)
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

    @Override
    public Map<String, String> getEnvMap() {
        return env;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getEnv() {
        return env.entrySet().stream()
                .map(it -> it.getKey() + "=" + it.getValue())
                .collect(Collectors.toList());
    }

    @Override
    public void setEnv(List<String> env) {
        this.env = env.stream()
                .map(it -> it.split("="))
                .collect(Collectors.toMap(
                        it -> it[0],
                        it -> it[1]
                ));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addEnv(String key, String value) {
        env.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFileSystemBind(final String hostPath, final String containerPath, final BindMode mode, final SelinuxContext selinuxContext) {

        final MountableFile mountableFile = MountableFile.forHostPath(hostPath);
        binds.add(new Bind(mountableFile.getResolvedPath(), new Volume(containerPath), mode.accessMode, selinuxContext.selContext));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withFileSystemBind(String hostPath, String containerPath, BindMode mode) {
        addFileSystemBind(hostPath, containerPath, mode);
        return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withVolumesFrom(Container container, BindMode mode) {
        addVolumesFrom(container, mode);
        return self();
    }

    private void addVolumesFrom(Container container, BindMode mode) {
        volumesFroms.add(new VolumesFrom(container.getContainerName(), mode.accessMode));
    }

    /**
     * @deprecated Links are deprecated (see <a href="https://github.com/testcontainers/testcontainers-java/issues/465">#465</a>). Please use {@link Network} features instead.
     */
    @Deprecated
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
        this.setExposedPorts(newArrayList(ports));
        return self();

    }

    /**
     * Add a TCP container port that should be bound to a fixed port on the docker host.
     * <p>
     * Note that this method is protected scope to discourage use, as clashes or instability are more likely when
     * using fixed port mappings. If you need to use this method from a test, please use {@link FixedHostPortGenericContainer}
     * instead of GenericContainer.
     *
     * @param hostPort
     * @param containerPort
     */
    protected void addFixedExposedPort(int hostPort, int containerPort) {
        addFixedExposedPort(hostPort, containerPort, InternetProtocol.TCP);
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
     * @param protocol
     */
    protected void addFixedExposedPort(int hostPort, int containerPort, InternetProtocol protocol) {
        portBindings.add(String.format("%d:%d/%s", hostPort, containerPort, protocol.toDockerNotation()));
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

    @Override
    public SELF withNetwork(Network network) {
        this.network = network;
        return self();
    }

    @Override
    public SELF withNetworkAliases(String... aliases) {
        Collections.addAll(this.networkAliases, aliases);
        return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withClasspathResourceMapping(final String resourcePath, final String containerPath, final BindMode mode) {
        return withClasspathResourceMapping(resourcePath, containerPath, mode, SelinuxContext.NONE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withClasspathResourceMapping(final String resourcePath, final String containerPath, final BindMode mode, final SelinuxContext selinuxContext) {
        final MountableFile mountableFile = MountableFile.forClasspathResource(resourcePath);

        this.addFileSystemBind(mountableFile.getResolvedPath(), containerPath, mode, selinuxContext);

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

    @Override
    public SELF withPrivilegedMode(boolean mode) {
        this.privilegedMode = mode;
        return self();
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
            throw new ContainerFetchException("Can't get Docker image: " + image, e);
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
    public SELF withLogConsumer(Consumer<OutputFrame> consumer) {
        this.logConsumers.add(consumer);

        return self();
    }

    /**
     * {@inheritDoc}
     */
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
    public void copyFileToContainer(MountableFile mountableLocalFile, String containerPath) {

        if (!isRunning()) {
            throw new IllegalStateException("copyFileToContainer can only be used while the Container is running");
        }

        this.dockerClient
                .copyArchiveToContainerCmd(this.containerId)
                .withHostResource(mountableLocalFile.getResolvedPath())
                .withRemotePath(containerPath)
                .exec();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFileFromContainer(String containerPath, String destinationPath) throws IOException {

        if (!isRunning()) {
            throw new IllegalStateException("copyFileToContainer can only be used while the Container is running");
        }

        try (final TarArchiveInputStream tarInputStream = new TarArchiveInputStream(this.dockerClient
                .copyArchiveFromContainerCmd(this.containerId, containerPath)
                .exec())) {
            tarInputStream.getNextTarEntry();
            IOUtils.copy(tarInputStream, new FileOutputStream(destinationPath));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecResult execInContainer(Charset outputCharset, String... command)
            throws UnsupportedOperationException, IOException, InterruptedException {
        return ExecInContainerPattern.execInContainer(getContainerInfo(), outputCharset, command);
    }

    /**
     * Allow container startup to be attempted more than once if an error occurs. To be if containers are
     * 'flaky' but this flakiness is not something that should affect test outcomes.
     *
     * @param attempts number of attempts
     */
    public SELF withStartupAttempts(int attempts) {
        this.startupAttempts = attempts;
        return self();
    }

    @Override
    public void close() {
        stop();
    }

    /**
     * Allow low level modifications of {@link CreateContainerCmd} after it was pre-configured in {@link #tryStart(Profiler)}.
     * Invocation happens eagerly on a moment when container is created.
     * Warning: this does expose the underlying docker-java API so might change outside of our control.
     *
     * @param modifier {@link Consumer} of {@link CreateContainerCmd}.
     * @return this
     */
    public SELF withCreateContainerCmdModifier(Consumer<CreateContainerCmd> modifier) {
        createContainerCmdModifiers.add(modifier);
        return self();
    }

    /**
     * Convenience class with access to non-public members of GenericContainer.
     *
     * @deprecated use {@link org.testcontainers.containers.wait.strategy.AbstractWaitStrategy}
     */
    @Deprecated
    public static abstract class AbstractWaitStrategy extends org.testcontainers.containers.wait.strategy.AbstractWaitStrategy implements WaitStrategy {
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
         * @deprecated see {@link AbstractWaitStrategy#getLivenessCheckPorts()}
         */
        @Deprecated
        protected Integer getLivenessCheckPort() {
            return container.getLivenessCheckPort();
        }

        /**
         * @return the ports on which to check if the container is ready
         */
        protected Set<Integer> getLivenessCheckPorts() {
            return container.getLivenessCheckPorts();
        }

        /**
         * @return the rate limiter to use
         */
        protected RateLimiter getRateLimiter() {
            return DOCKER_CLIENT_RATE_LIMITER;
        }
    }
}

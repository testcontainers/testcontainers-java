package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import com.google.common.base.Strings;
import lombok.*;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.slf4j.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.wait.Wait;
import org.testcontainers.containers.wait.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;
import org.testcontainers.utility.*;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.collect.Lists.newArrayList;
import static org.testcontainers.utility.CommandLine.runShellCommand;

/**
 * Base class for that allows a container to be launched and controlled.
 */
@Data
public class GenericContainer<SELF extends GenericContainer<SELF>>
        extends FailureDetectingExternalResource
        implements Container<SELF>, AutoCloseable, WaitStrategyTarget, Startable {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static final int CONTAINER_RUNNING_TIMEOUT_SEC = 30;

    public static final String INTERNAL_HOST_HOSTNAME = "host.testcontainers.internal";

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
    private Map<String, String> labels = new HashMap<>();

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

    /**
     * The shared memory size to use when starting the container.
     * This value is in bytes.
     */
    @Nullable
    private Long shmSize;

    private Map<MountableFile, String> copyToFileContainerPathMap = new HashMap<>();

    protected final Set<Startable> dependencies = new HashSet<>();

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

    /**
     * Set during container startup
     * // TODO make it private
     *
     * @deprecated use {@link ContainerState#getContainerId()}
     */
    @Setter(AccessLevel.NONE)
    @Deprecated
    protected String containerId;

    /**
     * Set during container startup
     *
     * @deprecated use {@link GenericContainer#getContainerInfo()}
     */
    @Setter(AccessLevel.NONE)
    @Deprecated
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

    @Nullable
    private Map<String, String> tmpFsMapping;


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
     * @see #dependsOn(List)
     */
    public SELF dependsOn(Startable... startables) {
        Collections.addAll(dependencies, startables);
        return self();
    }

    /**
     * Delays this container's creation and start until provided {@link Startable}s start first.
     * Note that the circular dependencies are not supported.
     *
     * @param startables a list of {@link Startable} to depend on
     * @see Startables#deepStart(Collection)
     */
    public SELF dependsOn(List<Startable> startables) {
        dependencies.addAll(startables);
        return self();
    }

    public String getContainerId() {
        return containerId;
    }

    /**
     * Starts the container using docker, pulling an image if necessary.
     */
    @Override
    @SneakyThrows({InterruptedException.class, ExecutionException.class})
    public void start() {
        if (containerId != null) {
            return;
        }
        Startables.deepStart(dependencies).get();
        doStart();
    }

    protected void doStart() {
        try {
            configure();

            logger().debug("Starting container: {}", getDockerImageName());
            logger().debug("Trying to start container: {}", image.get());

            AtomicInteger attempt = new AtomicInteger(0);
            Unreliables.retryUntilSuccess(startupAttempts, () -> {
                logger().debug("Trying to start container: {} (attempt {}/{})", image.get(), attempt.incrementAndGet(), startupAttempts);
                tryStart();
                return true;
            });

        } catch (Exception e) {
            throw new ContainerLaunchException("Container startup failed", e);
        }
    }

    private void tryStart() {
        try {
            String dockerImageName = image.get();
            logger().debug("Starting container: {}", dockerImageName);

            logger().info("Creating container for image: {}", dockerImageName);
            CreateContainerCmd createCommand = dockerClient.createContainerCmd(dockerImageName);
            applyConfiguration(createCommand);

            containerId = createCommand.exec().getId();

            connectToPortForwardingNetwork(createCommand.getNetworkMode());

            copyToFileContainerPathMap.forEach(this::copyFileToContainer);

            containerIsCreated(containerId);

            logger().info("Starting container with ID: {}", containerId);
            dockerClient.startContainerCmd(containerId).exec();

            logger().info("Container {} is starting: {}", dockerImageName, containerId);

            // For all registered output consumers, start following as close to container startup as possible
            this.logConsumers.forEach(this::followOutput);

            // Tell subclasses that we're starting
            containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
            containerName = containerInfo.getName();
            containerIsStarting(containerInfo);

            // Wait until the container has reached the desired running state
            if (!this.startupCheckStrategy.waitUntilStartupSuccessful(dockerClient, containerId)) {
                // Bail out, don't wait for the port to start listening.
                // (Exception thrown here will be caught below and wrapped)
                throw new IllegalStateException("Container did not start correctly.");
            }

            // Wait until the process within the container has become ready for use (e.g. listening on network, log message emitted, etc).
            waitUntilContainerStarted();

            logger().info("Container {} started", dockerImageName);
            containerIsStarted(containerInfo);
        } catch (Exception e) {
            logger().error("Could not start container", e);

            if (containerId != null) {
                // Log output if startup failed, either due to a container failure or exception (including timeout)
                final String containerLogs = getLogs();

                if (containerLogs.length() > 0) {
                    logger().error("Log output from the failed container:\n{}", getLogs());
                } else {
                    logger().error("There are no stdout/stderr logs available for the failed container");
                }
            }

            throw new ContainerLaunchException("Could not create/start container", e);
        }
    }

    /**
     * Set any custom settings for the create command such as shared memory size.
     */
    private HostConfig buildHostConfig() {
        HostConfig config = new HostConfig();
        if (shmSize != null) {
            config.withShmSize(shmSize);
        }
        if (tmpFsMapping != null) {
            config.withTmpFs(tmpFsMapping);
        }
        return config;
    }

    private void connectToPortForwardingNetwork(String networkMode) {
        PortForwardingContainer.INSTANCE.getNetwork().map(ContainerNetwork::getNetworkID).ifPresent(networkId -> {
            if (!Arrays.asList(networkId, "none", "host").contains(networkMode)) {
                dockerClient.connectToNetworkCmd().withContainerId(containerId).withNetworkId(networkId).exec();
            }
        });
    }

    /**
     * Stops the container.
     */
    @Override
    public void stop() {

        if (containerId == null) {
            return;
        }

        try {
            String imageName;

            try {
                imageName = image.get();
            } catch (Exception e) {
                imageName = "<unknown>";
            }

            containerIsStopping(containerInfo);
            ResourceReaper.instance().stopAndRemoveContainer(containerId, imageName);
            containerIsStopped(containerInfo);
        } finally {
            containerId = null;
            containerInfo = null;
        }
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
    protected void containerIsCreated(String containerId) {
    }

    @SuppressWarnings({"EmptyMethod", "UnusedParameters"})
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
    }

    @SuppressWarnings({"EmptyMethod", "UnusedParameters"})
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
    }

    /**
     * A hook that is executed before the container is stopped with {@link #stop()}.
     * Warning! This hook won't be executed if the container is terminated during
     * the JVM's shutdown hook or by Ryuk.
     */
    @SuppressWarnings({"EmptyMethod", "UnusedParameters"})
    protected void containerIsStopping(InspectContainerResponse containerInfo) {
    }

    /**
     * A hook that is executed after the container is stopped with {@link #stop()}.
     * Warning! This hook won't be executed if the container is terminated during
     * the JVM's shutdown hook or by Ryuk.
     */
    @SuppressWarnings({"EmptyMethod", "UnusedParameters"})
    protected void containerIsStopped(InspectContainerResponse containerInfo) {
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
        HostConfig hostConfig = buildHostConfig();
        createCommand.withHostConfig(hostConfig);

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
                .filter(it -> it.getValue() != null)
                .map(it -> it.getKey() + "=" + it.getValue())
                .toArray(String[]::new);
        createCommand.withEnv(envArray);

        boolean shouldCheckFileMountingSupport = binds.size() > 0 && !TestcontainersConfiguration.getInstance().isDisableChecks();
        if (shouldCheckFileMountingSupport) {
            if (!DockerClientFactory.instance().isFileMountingSupported()) {
                VisibleAssertions.warn(
                    "Unable to mount a file from test host into a running container. " +
                        "This may be a misconfiguration or limitation of your Docker environment. " +
                        "Some features might not work."
                );
            }
        }

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

        PortForwardingContainer.INSTANCE.getNetwork().ifPresent(it -> {
            withExtraHost(INTERNAL_HOST_HOSTNAME, it.getIpAddress());
        });

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

        Map<String, String> combinedLabels = new HashMap<>();
        combinedLabels.putAll(labels);
        if (createCommand.getLabels() != null) {
            combinedLabels.putAll(createCommand.getLabels());
        }
        combinedLabels.putAll(DockerClientFactory.DEFAULT_LABELS);

        createCommand.withLabels(combinedLabels);
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
        org.testcontainers.containers.wait.strategy.WaitStrategy waitStrategy = getWaitStrategy();
        if (waitStrategy != null) {
            waitStrategy.waitUntilReady(this);
        }
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

    private TestDescription toDescription(Description description) {
        return new TestDescription() {
            @Override
            public String getTestId() {
                return description.getDisplayName();
            }

            @Override
            public String getFilesystemFriendlyName() {
                return description.getClassName() + "-" + description.getMethodName();
            }
        };
    }

    @Override
    @Deprecated
    public Statement apply(Statement base, Description description) {
        return super.apply(base, description);
    }

    @Override
    @Deprecated
    protected void starting(Description description) {
        if (this instanceof TestLifecycleAware) {
            ((TestLifecycleAware) this).beforeTest(toDescription(description));
        }
        this.start();
    }

    @Override
    @Deprecated
    protected void succeeded(Description description) {
        if (this instanceof TestLifecycleAware) {
            ((TestLifecycleAware) this).afterTest(toDescription(description), Optional.empty());
        }
    }

    @Override
    @Deprecated
    protected void failed(Throwable e, Description description) {
        if (this instanceof TestLifecycleAware) {
            ((TestLifecycleAware) this).afterTest(toDescription(description), Optional.of(e));
        }
    }

    @Override
    @Deprecated
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
    public SELF withLabel(String key, String value) {
        if (key.startsWith("org.testcontainers")) {
            throw new IllegalArgumentException("The org.testcontainers namespace is reserved for interal use");
        }
        labels.put(key, value);
        return self();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SELF withLabels(Map<String, String> labels) {
        labels.forEach(this::withLabel);
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

        if (mode == BindMode.READ_ONLY && selinuxContext == SelinuxContext.NONE) {
            withCopyFileToContainer(mountableFile, containerPath);
        } else {
            addFileSystemBind(mountableFile.getResolvedPath(), containerPath, mode, selinuxContext);
        }

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
     * {@inheritDoc}
     */
    @Override
    public SELF withCopyFileToContainer(MountableFile mountableFile, String containerPath) {
        copyToFileContainerPathMap.put(mountableFile, containerPath);
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
    public void copyFileToContainer(MountableFile mountableFile, String containerPath) {
        File sourceFile = new File(mountableFile.getResolvedPath());

        if (containerPath.endsWith("/") && sourceFile.isFile()) {
            logger().warn("folder-like containerPath in copyFileToContainer is deprecated, please explicitly specify a file path");
            copyFileToContainer((Transferable) mountableFile, containerPath + sourceFile.getName());
        } else {
            copyFileToContainer((Transferable) mountableFile, containerPath);
        }
    }

    @Override
    @SneakyThrows(IOException.class)
    public void copyFileToContainer(Transferable transferable, String containerPath) {
        if (!isCreated()) {
            throw new IllegalStateException("copyFileToContainer can only be used with created / running container");
        }

        try (
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            TarArchiveOutputStream tarArchive = new TarArchiveOutputStream(byteArrayOutputStream)
        ) {
            tarArchive.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            int lastSlashIndex = StringUtils.removeEnd(containerPath, "/").lastIndexOf("/");
            String extractArchiveTo = containerPath.substring(0, lastSlashIndex + 1);
            String pathInArchive = containerPath.substring(lastSlashIndex + 1);
            transferable.transferTo(tarArchive, pathInArchive);
            tarArchive.finish();

            dockerClient
                .copyArchiveToContainerCmd(containerId)
                .withTarInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))
                .withRemotePath(extractArchiveTo)
                .exec();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copyFileFromContainer(String containerPath, String destinationPath) {
        copyFileFromContainer(containerPath, inputStream -> {
            try(FileOutputStream output = new FileOutputStream(destinationPath)) {
                IOUtils.copy(inputStream, output);
                return null;
            }
        });
    }

    @Override
    @SneakyThrows(Exception.class)
    public <T> T copyFileFromContainer(String containerPath, ThrowingFunction<InputStream, T> consumer) {
        if (!isCreated()) {
            throw new IllegalStateException("copyFileFromContainer can only be used when the Container is created.");
        }

        try (
            InputStream inputStream = dockerClient.copyArchiveFromContainerCmd(containerId, containerPath).exec();
            TarArchiveInputStream tarInputStream = new TarArchiveInputStream(inputStream)
        ) {
            tarInputStream.getNextTarEntry();
            return consumer.apply(tarInputStream);
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

    /**
     * Allow low level modifications of {@link CreateContainerCmd} after it was pre-configured in {@link #tryStart()}.
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
     * Size of /dev/shm
     * @param bytes The number of bytes to assign the shared memory. If null, it will apply the Docker default which is 64 MB.
     * @return this
     */
    public SELF withSharedMemorySize(Long bytes) {
        this.shmSize = bytes;
        return self();
    }

    /**
     * First class support for configuring tmpfs
     * @param mapping path and params of tmpfs/mount flag for container
     * @return this
     */
    public SELF withTmpFs(Map<String, String> mapping) {
        this.tmpFsMapping = mapping;
        return self();
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
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

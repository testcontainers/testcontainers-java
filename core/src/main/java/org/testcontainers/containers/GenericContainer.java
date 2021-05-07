package org.testcontainers.containers;

import static com.google.common.collect.Lists.newArrayList;
import static org.testcontainers.utility.CommandLine.runShellCommand;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Link;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.api.model.VolumesFrom;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.Adler32;
import java.util.zip.Checksum;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.UnstableAPI;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.IsRunningStartupCheckStrategy;
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;
import org.testcontainers.images.ImagePullPolicy;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.DockerLoggerFactory;
import org.testcontainers.utility.DockerMachineClient;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.PathUtils;
import org.testcontainers.utility.ResourceReaper;
import org.testcontainers.utility.TestcontainersConfiguration;

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

    static final String HASH_LABEL = "org.testcontainers.hash";

    static final String COPIED_FILES_HASH_LABEL = "org.testcontainers.copied_files.hash";

    /*
     * Default settings
     */
    @NonNull
    private LinkedHashSet<Integer> exposedPorts = new LinkedHashSet<>();

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
    private RemoteDockerImage image;

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

    // Maintain order in which entries are added, as earlier target location may be a prefix of a later location.
    private Map<MountableFile, String> copyToFileContainerPathMap = new LinkedHashMap<>();

    protected final Set<Startable> dependencies = new HashSet<>();

    /**
     * Unique instance of DockerClient for use by this container object.
     * We use {@link DockerClientFactory#lazyClient()} here to avoid eager client creation
     */
    @Setter(AccessLevel.NONE)
    protected DockerClient dockerClient = DockerClientFactory.lazyClient();

    /**
     * Set during container startup
     *
     */
    @Setter(AccessLevel.NONE)
    @VisibleForTesting
    String containerId;

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

    @Setter(AccessLevel.NONE)
    private boolean shouldBeReused = false;


    public GenericContainer(@NonNull final DockerImageName dockerImageName) {
        this.image = new RemoteDockerImage(dockerImageName);
    }

    public GenericContainer(@NonNull final RemoteDockerImage image) {
        this.image = image;
    }

    /**
     * @deprecated use {@link GenericContainer(DockerImageName)} instead
     */
    @Deprecated
    public GenericContainer() {
        this(TestcontainersConfiguration.getInstance().getTinyImage());
    }

    public GenericContainer(@NonNull final String dockerImageName) {
        this.setDockerImageName(dockerImageName);
    }

    public GenericContainer(@NonNull final Future<String> image) {
        setImage(image);
    }

    public void setImage(Future<String> image) {
        this.image = new RemoteDockerImage(image);
    }

    @Override
    public List<Integer> getExposedPorts() {
        return new ArrayList<>(exposedPorts);
    }

    @Override
    public void setExposedPorts(List<Integer> exposedPorts) {
        this.exposedPorts = new LinkedHashSet<>(exposedPorts);
    }

    /**
     * @see #dependsOn(Iterable)
     */
    public SELF dependsOn(Startable... startables) {
        Collections.addAll(dependencies, startables);
        return self();
    }

    /**
     * @see #dependsOn(Iterable)
     */
    public SELF dependsOn(List<? extends Startable> startables) {
        return this.dependsOn((Iterable<? extends Startable>) startables);
    }

    /**
     * Delays this container's creation and start until provided {@link Startable}s start first.
     * Note that the circular dependencies are not supported.
     *
     * @param startables a list of {@link Startable} to depend on
     * @see Startables#deepStart(Iterable)
     */
    public SELF dependsOn(Iterable<? extends Startable> startables) {
        startables.forEach(dependencies::add);
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
        // trigger LazyDockerClient's resolve so that we fail fast here and not in getDockerImageName()
        dockerClient.authConfig();
        doStart();
    }

    protected void doStart() {
        try {
            configure();

            Instant startedAt = Instant.now();

            logger().debug("Starting container: {}", getDockerImageName());

            AtomicInteger attempt = new AtomicInteger(0);
            Unreliables.retryUntilSuccess(startupAttempts, () -> {
                logger().debug("Trying to start container: {} (attempt {}/{})", getDockerImageName(), attempt.incrementAndGet(), startupAttempts);
                tryStart(startedAt);
                return true;
            });

        } catch (Exception e) {
            throw new ContainerLaunchException("Container startup failed", e);
        }
    }

    @UnstableAPI
    @SneakyThrows
    protected boolean canBeReused() {
        for (Class<?> type = getClass(); type != GenericContainer.class; type = type.getSuperclass()) {
            try {
                Method method = type.getDeclaredMethod("containerIsCreated", String.class);
                if (method.getDeclaringClass() != GenericContainer.class) {
                    logger().warn("{} can't be reused because it overrides {}", getClass(), method.getName());
                    return false;
                }
            } catch (NoSuchMethodException e) {
                // ignore
            }
        }

        return true;
    }

    private void tryStart(Instant startedAt) {
        try {
            String dockerImageName = getDockerImageName();
            logger().debug("Starting container: {}", dockerImageName);

            logger().info("Creating container for image: {}", dockerImageName);
            CreateContainerCmd createCommand = dockerClient.createContainerCmd(dockerImageName);
            applyConfiguration(createCommand);

            createCommand.getLabels().put(DockerClientFactory.TESTCONTAINERS_LABEL, "true");

            boolean reused = false;
            final boolean reusable;
            if (shouldBeReused) {
                if (!canBeReused()) {
                    throw new IllegalStateException("This container does not support reuse");
                }

                if (TestcontainersConfiguration.getInstance().environmentSupportsReuse()) {
                    createCommand.getLabels().put(
                        COPIED_FILES_HASH_LABEL,
                        Long.toHexString(hashCopiedFiles().getValue())
                    );

                    String hash = hash(createCommand);

                    containerId = findContainerForReuse(hash).orElse(null);

                    if (containerId != null) {
                        logger().info("Reusing container with ID: {} and hash: {}", containerId, hash);
                        reused = true;
                    } else {
                        logger().debug("Can't find a reusable running container with hash: {}", hash);

                        createCommand.getLabels().put(HASH_LABEL, hash);
                    }
                    reusable = true;
                } else {
                    logger().warn(
                        "" +
                            "Reuse was requested but the environment does not support the reuse of containers\n" +
                            "To enable reuse of containers, you must set 'testcontainers.reuse.enable=true' in a file located at {}",
                        Paths.get(System.getProperty("user.home"), ".testcontainers.properties")
                    );
                    reusable = false;
                }
            } else {
                reusable = false;
            }

            if (!reusable) {
                createCommand.getLabels().put(DockerClientFactory.TESTCONTAINERS_SESSION_ID_LABEL, DockerClientFactory.SESSION_ID);
            }

            if (!reused) {
                containerId = createCommand.exec().getId();

                // TODO use single "copy" invocation (and calculate an hash of the resulting tar archive)
                copyToFileContainerPathMap.forEach(this::copyFileToContainer);
            }

            connectToPortForwardingNetwork(createCommand.getNetworkMode());

            if (!reused) {
                containerIsCreated(containerId);

                logger().info("Starting container with ID: {}", containerId);
                dockerClient.startContainerCmd(containerId).exec();
            }

            logger().info("Container {} is starting: {}", dockerImageName, containerId);

            // For all registered output consumers, start following as close to container startup as possible
            this.logConsumers.forEach(this::followOutput);

            // Tell subclasses that we're starting
            containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
            containerIsStarting(containerInfo, reused);

            // Wait until the container has reached the desired running state
            if (!this.startupCheckStrategy.waitUntilStartupSuccessful(dockerClient, containerId)) {
                // Bail out, don't wait for the port to start listening.
                // (Exception thrown here will be caught below and wrapped)
                throw new IllegalStateException("Container did not start correctly.");
            }

            // Wait until the process within the container has become ready for use (e.g. listening on network, log message emitted, etc).
            try {
                waitUntilContainerStarted();
            } catch (Exception e) {
                logger().debug("Wait strategy threw an exception", e);
                InspectContainerResponse inspectContainerResponse = null;
                try {
                    inspectContainerResponse = dockerClient.inspectContainerCmd(containerId).exec();
                } catch (NotFoundException notFoundException) {
                    logger().debug("Container {} not found", containerId, notFoundException);
                }

                if (inspectContainerResponse == null) {
                    throw new IllegalStateException("Container is removed");
                }

                InspectContainerResponse.ContainerState state = inspectContainerResponse.getState();
                if (Boolean.TRUE.equals(state.getDead())) {
                    throw new IllegalStateException("Container is dead");
                }

                if (Boolean.TRUE.equals(state.getOOMKilled())) {
                    throw new IllegalStateException("Container crashed with out-of-memory (OOMKilled)");
                }

                String error = state.getError();
                if (!StringUtils.isBlank(error)) {
                    throw new IllegalStateException("Container crashed: " + error);
                }

                if (!Boolean.TRUE.equals(state.getRunning())) {
                    throw new IllegalStateException("Container exited with code " + state.getExitCode());
                }

                throw e;
            }

            logger().info("Container {} started in {}", dockerImageName, Duration.between(startedAt, Instant.now()));
            containerIsStarted(containerInfo, reused);
        } catch (Exception e) {
            if (e instanceof UndeclaredThrowableException && e.getCause() instanceof Exception) {
                e = (Exception) e.getCause();
            }
            if (e instanceof InvocationTargetException && e.getCause() instanceof Exception) {
                e = (Exception) e.getCause();
            }
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

    @VisibleForTesting
    Checksum hashCopiedFiles() {
        Checksum checksum = new Adler32();
        copyToFileContainerPathMap.entrySet().stream().sorted(Entry.comparingByValue()).forEach(entry -> {
            byte[] pathBytes = entry.getValue().getBytes();
            // Add path to the hash
            checksum.update(pathBytes, 0, pathBytes.length);

            File file = new File(entry.getKey().getResolvedPath());
            checksumFile(file, checksum);
        });
        return checksum;
    }

    @VisibleForTesting
    @SneakyThrows(IOException.class)
    void checksumFile(File file, Checksum checksum) {
        Path path = file.toPath();
        checksum.update(MountableFile.getUnixFileMode(path));
        if (file.isDirectory()) {
            try (Stream<Path> stream = Files.walk(path)) {
                stream.filter(it -> it != path).forEach(it -> {
                    checksumFile(it.toFile(), checksum);
                });
            }
        } else {
            FileUtils.checksum(file, checksum);
        }
    }

    @UnstableAPI
    @SneakyThrows(JsonProcessingException.class)
    final String hash(CreateContainerCmd createCommand) {
        DefaultDockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

        byte[] commandJson = dockerClientConfig.getObjectMapper()
            .copy()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .writeValueAsBytes(createCommand);

        // TODO add Testcontainers' version to the hash
        return Hashing.sha1().hashBytes(commandJson).toString();
    }

    @VisibleForTesting
    Optional<String> findContainerForReuse(String hash) {
        // TODO locking
        return dockerClient.listContainersCmd()
            .withLabelFilter(ImmutableMap.of(HASH_LABEL, hash))
            .withLimit(1)
            .withStatusFilter(Arrays.asList("running"))
            .exec()
            .stream()
            .findAny()
            .map(it -> it.getId());
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
                imageName = getDockerImageName();
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
    @UnstableAPI
    protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
        containerIsStarting(containerInfo);
    }

    @SuppressWarnings({"EmptyMethod", "UnusedParameters"})
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
    }

    @SuppressWarnings({"EmptyMethod", "UnusedParameters"})
    @UnstableAPI
    protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
        containerIsStarted(containerInfo);
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
        Iterator<Integer> exposedPortsIterator = exposedPorts.iterator();
        if (exposedPortsIterator.hasNext()) {
            return getMappedPort(exposedPortsIterator.next());
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
                logger().warn(
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
        for (Entry<String, LinkableContainer> linkEntries : linkedContainers.entrySet()) {

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
        if (SystemUtils.IS_OS_WINDOWS && hostPath.startsWith("/")) {
            // e.g. Docker socket mount
            binds.add(new Bind(hostPath, new Volume(containerPath), mode.accessMode, selinuxContext.selContext));

        } else {
            final MountableFile mountableFile = MountableFile.forHostPath(hostPath);
            binds.add(new Bind(mountableFile.getResolvedPath(), new Volume(containerPath), mode.accessMode, selinuxContext.selContext));
        }
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

    @Override
    public SELF withImagePullPolicy(ImagePullPolicy imagePullPolicy) {
        this.image = this.image.withImagePullPolicy(imagePullPolicy);
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
        return getHost();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDockerImageName(@NonNull String dockerImageName) {
        this.image = new RemoteDockerImage(dockerImageName);
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
    @SneakyThrows
    public void copyFileFromContainer(String containerPath, String destinationPath) {
        Container.super.copyFileFromContainer(containerPath, destinationPath);
    }

    /**
     * Allow container startup to be attempted more than once if an error occurs. To be used if containers are
     * 'flaky' but this flakiness is not something that should affect test outcomes.
     *
     * @param attempts number of attempts
     */
    public SELF withStartupAttempts(int attempts) {
        this.startupAttempts = attempts;
        return self();
    }

    /**
     * Allow low level modifications of {@link CreateContainerCmd} after it was pre-configured in {@link #tryStart(Instant)}.
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

    @UnstableAPI
    public SELF withReuse(boolean reusable) {
        this.shouldBeReused = reusable;
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

    @Override
    public String getContainerName() {
        return getContainerInfo().getName();
    }
}

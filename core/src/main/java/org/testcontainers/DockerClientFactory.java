package org.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Version;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.DockerMachineClientProviderStrategy;
import org.testcontainers.images.TimeLimitedLoggedPullImageResultCallback;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.ResourceReaper;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Singleton class that provides initialized Docker clients.
 * <p>
 * The correct client configuration to use will be determined on first use, and cached thereafter.
 */
@Slf4j
public class DockerClientFactory {

    public static final ThreadGroup TESTCONTAINERS_THREAD_GROUP = new ThreadGroup("testcontainers");
    public static final String TESTCONTAINERS_LABEL = DockerClientFactory.class.getPackage().getName();
    public static final String TESTCONTAINERS_SESSION_ID_LABEL = TESTCONTAINERS_LABEL + ".sessionId";

    public static final String SESSION_ID = UUID.randomUUID().toString();

    public static final Map<String, String> DEFAULT_LABELS = ImmutableMap.of(
            TESTCONTAINERS_LABEL, "true",
            TESTCONTAINERS_SESSION_ID_LABEL, SESSION_ID
    );

    private static final String TINY_IMAGE = TestcontainersConfiguration.getInstance().getTinyImage();
    private static DockerClientFactory instance;

    // Cached client configuration
    @VisibleForTesting
    DockerClientProviderStrategy strategy;

    @VisibleForTesting
    DockerClient dockerClient;

    private String activeApiVersion;
    private String activeExecutionDriver;
    private RuntimeException cachedChecksFailure;

    @Getter(lazy = true)
    private final boolean fileMountingSupported = checkMountableFile();


    static {
        System.setProperty("org.testcontainers.shaded.io.netty.packagePrefix", "org.testcontainers.shaded.");
    }

    private String ryukContainerId = null;

    @VisibleForTesting
    DockerClientFactory() {

    }

    public static DockerClient lazyClient() {
        return LazyDockerClient.INSTANCE;
    }

    /**
     * Obtain an instance of the DockerClientFactory.
     *
     * @return the singleton instance of DockerClientFactory
     */
    public synchronized static DockerClientFactory instance() {
        if (instance == null) {
            instance = new DockerClientFactory();
        }

        return instance;
    }

    @Synchronized
    private DockerClientProviderStrategy getOrInitializeStrategy() {
        if (strategy != null) {
            return strategy;
        }

        List<DockerClientProviderStrategy> configurationStrategies = new ArrayList<>();
        ServiceLoader.load(DockerClientProviderStrategy.class).forEach(configurationStrategies::add);

        strategy = DockerClientProviderStrategy.getFirstValidStrategy(configurationStrategies);
        return strategy;
    }

    /**
     *
     * @return a new initialized Docker client
     */
    @Synchronized
    public DockerClient client() {

        if (dockerClient != null) {
            return dockerClient;
        }

        final DockerClientProviderStrategy strategy = getOrInitializeStrategy();

        String hostIpAddress = strategy.getDockerHostIpAddress();
        log.info("Docker host IP address is {}", hostIpAddress);
        final DockerClient client = strategy.getClient();

        Info dockerInfo = client.infoCmd().exec();
        Version version = client.versionCmd().exec();
        activeApiVersion = version.getApiVersion();
        activeExecutionDriver = dockerInfo.getExecutionDriver();
        log.info("Connected to docker: \n" +
                "  Server Version: " + dockerInfo.getServerVersion() + "\n" +
                "  API Version: " + activeApiVersion + "\n" +
                "  Operating System: " + dockerInfo.getOperatingSystem() + "\n" +
                "  Total Memory: " + dockerInfo.getMemTotal() / (1024 * 1024) + " MB");

        boolean useRyuk = !Boolean.parseBoolean(System.getenv("TESTCONTAINERS_RYUK_DISABLED"));
        if (useRyuk && ryukContainerId == null) {
            ryukContainerId = ResourceReaper.start(hostIpAddress, client);
            log.info("Ryuk started - will monitor and terminate Testcontainers containers on JVM exit");
        }

        boolean checksEnabled = !TestcontainersConfiguration.getInstance().isDisableChecks();
        if (checksEnabled) {
            // fail-fast if checks have failed previously
            if (cachedChecksFailure != null) {
                throw cachedChecksFailure;
            }

            try {
                log.info("Checking the system...");
                checkDockerVersion(version.getVersion());
                if (ryukContainerId != null) {
                    checkDiskSpace(client, ryukContainerId);
                } else {
                    runInsideDocker(
                        client,
                        createContainerCmd -> {
                            createContainerCmd.withName("testcontainers-checks-" + SESSION_ID);
                            createContainerCmd.getHostConfig().withAutoRemove(true);
                            createContainerCmd.withCmd("tail", "-f", "/dev/null");
                        },
                        (__, containerId) -> {
                            checkDiskSpace(client, containerId);
                            return "";
                        }
                    );
                }
            } catch (RuntimeException e) {
                cachedChecksFailure = e;
                throw e;
            }
        }

        dockerClient = client;
        return dockerClient;
    }

    private void checkDockerVersion(String dockerVersion) {
        boolean versionIsSufficient = new ComparableVersion(dockerVersion).compareTo(new ComparableVersion("1.6.0")) >= 0;
        check("Docker server version should be at least 1.6.0", versionIsSufficient);
    }

    private void checkDiskSpace(DockerClient dockerClient, String id) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            dockerClient
                    .execStartCmd(dockerClient.execCreateCmd(id).withAttachStdout(true).withCmd("df", "-P").exec().getId())
                    .exec(new ExecStartResultCallback(outputStream, null))
                    .awaitCompletion();
        } catch (Exception e) {
            log.debug("Can't exec disk checking command", e);
        }

        DiskSpaceUsage df = parseAvailableDiskSpace(outputStream.toString());

        check(
                "Docker environment should have more than 2GB free disk space",
                df.availableMB.map(it -> it >= 2048).orElse(true)
        );
    }

    private void check(String message, boolean isSuccessful) {
        if (isSuccessful) {
            log.info("✔︎ {}", message);
        } else {
            log.error("❌ {}", message);
            throw new IllegalStateException("Check failed: " + message);
        }
    }

    private boolean checkMountableFile() {
        DockerClient dockerClient = client();

        MountableFile mountableFile = MountableFile.forClasspathResource(ResourceReaper.class.getName().replace(".", "/") + ".class");

        Volume volume = new Volume("/dummy");
        try {
            return runInsideDocker(
                createContainerCmd -> createContainerCmd.withBinds(new Bind(mountableFile.getResolvedPath(), volume, AccessMode.ro)),
                (__, containerId) -> {
                    try (InputStream stream = dockerClient.copyArchiveFromContainerCmd(containerId, volume.getPath()).exec()) {
                        stream.read();
                        return true;
                    } catch (Exception e) {
                        return false;
                    }
                }
            );
        } catch (Exception e) {
            log.debug("Failure while checking for mountable file support", e);
            return false;
        }
    }

    /**
   * Check whether the image is available locally and pull it otherwise
   */
    @SneakyThrows
    public void checkAndPullImage(DockerClient client, String image) {
        List<Image> images = client.listImagesCmd().withImageNameFilter(image).exec();
        if (images.isEmpty()) {
            client.pullImageCmd(image).exec(new TimeLimitedLoggedPullImageResultCallback(log)).awaitCompletion();
        }
    }

    /**
     * @return the IP address of the host running Docker
     */
    public String dockerHostIpAddress() {
        return getOrInitializeStrategy().getDockerHostIpAddress();
    }

    public <T> T runInsideDocker(Consumer<CreateContainerCmd> createContainerCmdConsumer, BiFunction<DockerClient, String, T> block) {
        // We can't use client() here because it might create an infinite loop
        return runInsideDocker(getOrInitializeStrategy().getClient(), createContainerCmdConsumer, block);
    }

    private <T> T runInsideDocker(DockerClient client, Consumer<CreateContainerCmd> createContainerCmdConsumer, BiFunction<DockerClient, String, T> block) {
        checkAndPullImage(client, TINY_IMAGE);
        CreateContainerCmd createContainerCmd = client.createContainerCmd(TINY_IMAGE)
                .withLabels(DEFAULT_LABELS);
        createContainerCmdConsumer.accept(createContainerCmd);
        String id = createContainerCmd.exec().getId();

        try {
            client.startContainerCmd(id).exec();
            return block.apply(client, id);
        } finally {
            try {
                client.removeContainerCmd(id).withRemoveVolumes(true).withForce(true).exec();
            } catch (NotFoundException | InternalServerErrorException e) {
                log.debug("Swallowed exception while removing container", e);
            }
        }
    }

    @VisibleForTesting
    static class DiskSpaceUsage {
        Optional<Long> availableMB = Optional.empty();
        Optional<Integer> usedPercent = Optional.empty();
    }

    @VisibleForTesting
    DiskSpaceUsage parseAvailableDiskSpace(String dfOutput) {
        DiskSpaceUsage df = new DiskSpaceUsage();
        String[] lines = dfOutput.split("\n");
        for (String line : lines) {
            String[] fields = line.split("\\s+");
            if (fields.length > 5 && fields[5].equals("/")) {
                long availableKB = Long.parseLong(fields[3]);
                df.availableMB = Optional.of(availableKB / 1024L);
                df.usedPercent = Optional.of(Integer.valueOf(fields[4].replace("%", "")));
                break;
            }
        }
        return df;
    }

    /**
     * @return the docker API version of the daemon that we have connected to
     */
    public String getActiveApiVersion() {
        client();
        return activeApiVersion;
    }

    /**
     * @return the docker execution driver of the daemon that we have connected to
     */
    public String getActiveExecutionDriver() {
        client();
        return activeExecutionDriver;
    }

    /**
     * @param providerStrategyClass a class that extends {@link DockerMachineClientProviderStrategy}
     * @return whether or not the currently active strategy is of the provided type
     */
    public boolean isUsing(Class<? extends DockerClientProviderStrategy> providerStrategyClass) {
        return strategy != null && providerStrategyClass.isAssignableFrom(this.strategy.getClass());
    }
}

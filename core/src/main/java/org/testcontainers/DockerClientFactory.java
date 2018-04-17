package org.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.dockerclient.DockerClientProviderStrategy;
import org.testcontainers.dockerclient.DockerMachineClientProviderStrategy;
import org.testcontainers.utility.ComparableVersion;
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
    private DockerClientProviderStrategy strategy;
    private boolean initialized = false;
    private String activeApiVersion;
    private String activeExecutionDriver;

    static {
        System.setProperty("org.testcontainers.shaded.io.netty.packagePrefix", "org.testcontainers.shaded.");
    }

    /**
     * Private constructor
     */
    private DockerClientFactory() {

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

    /**
     *
     * @return a new initialized Docker client
     */
    @Synchronized
    public DockerClient client() {

        if (strategy != null) {
            return strategy.getClient();
        }

        List<DockerClientProviderStrategy> configurationStrategies = new ArrayList<DockerClientProviderStrategy>();
        ServiceLoader.load(DockerClientProviderStrategy.class).forEach( cs -> configurationStrategies.add( cs ) );

        strategy = DockerClientProviderStrategy.getFirstValidStrategy(configurationStrategies);

        String hostIpAddress = strategy.getDockerHostIpAddress();
        log.info("Docker host IP address is {}", hostIpAddress);
        DockerClient client = strategy.getClient();

        if (!initialized) {
            Info dockerInfo = client.infoCmd().exec();
            Version version = client.versionCmd().exec();
            activeApiVersion = version.getApiVersion();
            activeExecutionDriver = dockerInfo.getExecutionDriver();
            log.info("Connected to docker: \n" +
                    "  Server Version: " + dockerInfo.getServerVersion() + "\n" +
                    "  API Version: " + activeApiVersion + "\n" +
                    "  Operating System: " + dockerInfo.getOperatingSystem() + "\n" +
                    "  Total Memory: " + dockerInfo.getMemTotal() / (1024 * 1024) + " MB");

            boolean checksEnabled = !TestcontainersConfiguration.getInstance().isDisableChecks();

            String ryukContainerId = ResourceReaper.start(hostIpAddress, client, checksEnabled);
            log.info("Ryuk started - will monitor and terminate Testcontainers containers on JVM exit");

            VisibleAssertions.info("Checking the system...");

            checkDockerVersion(version.getVersion());

            if (checksEnabled) {
                checkDiskSpace(client, ryukContainerId);
                checkMountableFile(client, ryukContainerId);
            }

            initialized = true;
        }

        return client;
    }

    private void checkDockerVersion(String dockerVersion) {
        VisibleAssertions.assertThat("Docker version", dockerVersion, new BaseMatcher<String>() {
            @Override
            public boolean matches(Object o) {
                return new ComparableVersion(o.toString()).compareTo(new ComparableVersion("1.6.0")) >= 0;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("should be at least 1.6.0");
            }
        });
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

        VisibleAssertions.assertTrue(
                "Docker environment should have more than 2GB free disk space",
                df.availableMB.map(it -> it >= 2048).orElse(true)
        );
    }

    private void checkMountableFile(DockerClient dockerClient, String id) {
        try (InputStream stream = dockerClient.copyArchiveFromContainerCmd(id, "/dummy").exec()) {
            stream.read();
            VisibleAssertions.pass("File should be mountable");
        } catch (Exception e) {
            VisibleAssertions.fail("File should be mountable but fails with " + e.getMessage());
        }
    }

    /**
   * Check whether the image is available locally and pull it otherwise
   */
    public void checkAndPullImage(DockerClient client, String image) {
        List<Image> images = client.listImagesCmd().withImageNameFilter(image).exec();
        if (images.isEmpty()) {
            client.pullImageCmd(image).exec(new PullImageResultCallback()).awaitSuccess();
        }
    }

    /**
     * @return the IP address of the host running Docker
     */
    public String dockerHostIpAddress() {
        return strategy.getDockerHostIpAddress();
    }

    public <T> T runInsideDocker(Consumer<CreateContainerCmd> createContainerCmdConsumer, BiFunction<DockerClient, String, T> block) {
        if (strategy == null) {
            client();
        }
        // We can't use client() here because it might create an infinite loop
        return runInsideDocker(strategy.getClient(), createContainerCmdConsumer, block);
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
            } catch (NotFoundException | InternalServerErrorException ignored) {
                log.debug("", ignored);
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
                long availableKB = Long.valueOf(fields[3]);
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
        if (!initialized) {
            client();
        }
        return activeApiVersion;
    }

    /**
     * @return the docker execution driver of the daemon that we have connected to
     */
    public String getActiveExecutionDriver() {
        if (!initialized) {
            client();
        }
        return activeExecutionDriver;
    }

    /**
     * @param providerStrategyClass a class that extends {@link DockerMachineClientProviderStrategy}
     * @return whether or not the currently active strategy is of the provided type
     */
    public boolean isUsing(Class<? extends DockerClientProviderStrategy> providerStrategyClass) {
        return providerStrategyClass.isAssignableFrom(this.strategy.getClass());
    }

    private static class NotEnoughDiskSpaceException extends RuntimeException {
        NotEnoughDiskSpaceException(String message) {
            super(message);
        }
    }
}

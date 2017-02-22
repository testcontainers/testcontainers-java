package org.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Version;
import com.github.dockerjava.core.command.PullImageResultCallback;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.dockerclient.*;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static java.util.Arrays.asList;

/**
 * Singleton class that provides initialized Docker clients.
 * <p>
 * The correct client configuration to use will be determined on first use, and cached thereafter.
 */
@Slf4j
public class DockerClientFactory {

    private static final String TINY_IMAGE = TestcontainersConfiguration.getInstance().getTinyImage();
    private static DockerClientFactory instance;

    // Cached client configuration
    private DockerClientProviderStrategy strategy;
    private boolean preconditionsChecked = false;

    private static final List<DockerClientProviderStrategy> CONFIGURATION_STRATEGIES =
            asList(new EnvironmentAndSystemPropertyClientProviderStrategy(),
                    new UnixSocketClientProviderStrategy(),
                    new ProxiedUnixSocketClientProviderStrategy(),
                    new DockerMachineClientProviderStrategy(),
                    new NamedPipeSocketClientProviderStrategy());
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

        strategy = DockerClientProviderStrategy.getFirstValidStrategy(CONFIGURATION_STRATEGIES);

        log.info("Docker host IP address is {}", strategy.getDockerHostIpAddress());
        DockerClient client = strategy.getClient();

        if (!preconditionsChecked) {
            Info dockerInfo = client.infoCmd().exec();
            Version version = client.versionCmd().exec();
            activeApiVersion = version.getApiVersion();
            activeExecutionDriver = dockerInfo.getExecutionDriver();
            log.info("Connected to docker: \n" +
                    "  Server Version: " + dockerInfo.getServerVersion() + "\n" +
                    "  API Version: " + activeApiVersion + "\n" +
                    "  Operating System: " + dockerInfo.getOperatingSystem() + "\n" +
                    "  Total Memory: " + dockerInfo.getMemTotal() / (1024 * 1024) + " MB");

            checkVersion(version.getVersion());

            List<Image> images = client.listImagesCmd().exec();
            // Pull the image we use to perform some checks
            if (images.stream().noneMatch(it -> it.getRepoTags() != null && asList(it.getRepoTags()).contains(TINY_IMAGE))) {
                client.pullImageCmd(TINY_IMAGE).exec(new PullImageResultCallback()).awaitSuccess();
            }

            checkDiskSpaceAndHandleExceptions(client);
            preconditionsChecked = true;
        }

        return client;
    }

    /**
     * @return the IP address of the host running Docker
     */
    public String dockerHostIpAddress() {
        return strategy.getDockerHostIpAddress();
    }

    private void checkVersion(String version) {
        String[] splitVersion = version.split("\\.");
        if (Integer.valueOf(splitVersion[0]) <= 1 && Integer.valueOf(splitVersion[1]) < 6) {
            throw new IllegalStateException("Docker version 1.6.0+ is required, but version " + version + " was found");
        }
    }

    private void checkDiskSpaceAndHandleExceptions(DockerClient client) {
        try {
            checkDiskSpace(client);
        } catch (NotEnoughDiskSpaceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Encountered and ignored error while checking disk space", e);
        }
    }

    /**
     * Check whether this docker installation is likely to have disk space problems
     * @param client an active Docker client
     */
    private void checkDiskSpace(DockerClient client) {
        DiskSpaceUsage df = runInsideDocker(client, cmd -> cmd.withCmd("df", "-P"), (dockerClient, id) -> {
                String logResults = dockerClient.logContainerCmd(id)
                    .withStdOut(true)
                    .exec(new LogToStringContainerCallback())
                    .toString();

                return parseAvailableDiskSpace(logResults);
        });

        log.info("Disk utilization in Docker environment is {} ({} )",
            df.usedPercent.map(x -> x + "%").orElse("unknown"),
            df.availableMB.map(x -> x + " MB available").orElse("unknown available"));

        if (df.availableMB.map(it -> it < 2048).orElse(false)) {
            log.error("Docker environment has less than 2GB free - execution is unlikely to succeed so will be aborted.");
            throw new NotEnoughDiskSpaceException("Not enough disk space in Docker environment");
        }
    }

    public <T> T runInsideDocker(Consumer<CreateContainerCmd> createContainerCmdConsumer, BiFunction<DockerClient, String, T> block) {
        if (strategy == null) {
            client();
        }
        // We can't use client() here because it might create an infinite loop
        return runInsideDocker(strategy.getClient(), createContainerCmdConsumer, block);
    }

    private <T> T runInsideDocker(DockerClient client, Consumer<CreateContainerCmd> createContainerCmdConsumer, BiFunction<DockerClient, String, T> block) {
        CreateContainerCmd createContainerCmd = client.createContainerCmd(TINY_IMAGE);
        createContainerCmdConsumer.accept(createContainerCmd);
        String id = createContainerCmd.exec().getId();

        client.startContainerCmd(id).exec();

        try {
            return block.apply(client, id);
        } finally {
            try {
                client.removeContainerCmd(id).withRemoveVolumes(true).withForce(true).exec();
            } catch (NotFoundException | InternalServerErrorException ignored) {
                log.debug("", ignored);
            }
        }
    }
    
    private static class DiskSpaceUsage {
        Optional<Integer> availableMB = Optional.empty();
        Optional<Integer> usedPercent = Optional.empty();
    }
    
    private DiskSpaceUsage parseAvailableDiskSpace(String dfOutput) {
        DiskSpaceUsage df = new DiskSpaceUsage();
        String[] lines = dfOutput.split("\n");
        for (String line : lines) {
            String[] fields = line.split("\\s+");
            if (fields[5].equals("/")) {
                int availableKB = Integer.valueOf(fields[3]);
                df.availableMB = Optional.of(availableKB / 1024);
                df.usedPercent = Optional.of(Integer.valueOf(fields[4].replace("%", "")));
            }
        }
        return df;
    }

    /**
     * @return the docker API version of the daemon that we have connected to
     */
    public String getActiveApiVersion() {
        if (!preconditionsChecked) {
            client();
        }
        return activeApiVersion;
    }

    /**
     * @return the docker execution driver of the daemon that we have connected to
     */
    public String getActiveExecutionDriver() {
        if (!preconditionsChecked) {
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

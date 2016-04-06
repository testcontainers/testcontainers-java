package org.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.api.NotFoundException;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import lombok.Synchronized;
import org.slf4j.Logger;
import org.testcontainers.dockerclient.DockerClientConfigUtils;
import org.testcontainers.dockerclient.DockerConfigurationStrategy;
import org.testcontainers.dockerclient.DockerMachineConfigurationStrategy;
import org.testcontainers.dockerclient.EnvironmentAndSystemPropertyConfigurationStrategy;
import org.testcontainers.dockerclient.UnixSocketConfigurationStrategy;

import java.util.List;

import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Singleton class that provides initialized Docker clients.
 * <p>
 * The correct client configuration to use will be determined on first use, and cached thereafter.
 */
public class DockerClientFactory {

    private static DockerClientFactory instance;
    private static final Logger LOGGER = getLogger(DockerClientFactory.class);

    // Cached client configuration
    private DockerClientConfig config;
    private boolean preconditionsChecked = false;

    private static final List<DockerConfigurationStrategy> CONFIGURATION_STRATEGIES =
            asList(new EnvironmentAndSystemPropertyConfigurationStrategy(),
                    new DockerMachineConfigurationStrategy(),
                    new UnixSocketConfigurationStrategy());

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
    public DockerClient client() {
        return client(true);
    }

    /**
     *
     * @param failFast fail if client fails to ping Docker daemon
     * @return a new initialized Docker client
     */
    @Synchronized
    public DockerClient client(boolean failFast) {
        if (config == null) {
            config = DockerConfigurationStrategy.getFirstValidConfig(CONFIGURATION_STRATEGIES);
        }

        DockerClient client = DockerClientBuilder.getInstance(config).build();

        if (!preconditionsChecked) {
            String version = client.versionCmd().exec().getVersion();
            checkVersion(version);
            checkDiskSpaceAndHandleExceptions(client);
            preconditionsChecked = true;
        }

        if (failFast) {
            // Ping, to fail fast if our docker environment has gone away
            client.pingCmd().exec();
        }

        return client;
    }

    /**
     * @param config docker client configuration to extract the host IP address from
     * @return the IP address of the host running Docker
     */
    private String dockerHostIpAddress(DockerClientConfig config) {
        return DockerClientConfigUtils.getDockerHostIpAddress(config);
    }

    /**
     * @return the IP address of the host running Docker
     */
    public String dockerHostIpAddress() {
        return dockerHostIpAddress(config);
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
            LOGGER.warn("Encountered and ignored error while checking disk space", e);
        }
    }

    /**
     * Check whether this docker installation is likely to have disk space problems
     * @param client an active Docker client
     */
    private void checkDiskSpace(DockerClient client) {

        List<Image> images = client.listImagesCmd().exec();
        if (!images.stream().anyMatch(it -> asList(it.getRepoTags()).contains("alpine:3.2"))) {
            PullImageResultCallback callback = client.pullImageCmd("alpine:3.2").exec(new PullImageResultCallback());
            callback.awaitSuccess();
        }

        CreateContainerResponse createContainerResponse = client.createContainerCmd("alpine:3.2").withCmd("df", "-P").exec();
        String id = createContainerResponse.getId();

        client.startContainerCmd(id).exec();

        client.waitContainerCmd(id).exec();

        LogContainerResultCallback callback = client.logContainerCmd(id).withStdOut().exec(new LogContainerCallback());
        try {
            callback.awaitCompletion();
            String logResults = callback.toString();

            int availableKB = 0;
            int use = 0;
            String[] lines = logResults.split("\n");
            for (String line : lines) {
                String[] fields = line.split("\\s+");
                if (fields[5].equals("/")) {
                    availableKB = Integer.valueOf(fields[3]);
                    use = Integer.valueOf(fields[4].replace("%", ""));
                }
            }
            int availableMB = availableKB / 1024;

            LOGGER.info("Disk utilization in Docker environment is {}% ({} MB available)", use, availableMB);

            if (availableMB < 2048) {
                LOGGER.error("Docker environment has less than 2GB free - execution is unlikely to succeed so will be aborted.");
                throw new NotEnoughDiskSpaceException("Not enough disk space in Docker environment");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                client.removeContainerCmd(id).withRemoveVolumes(true).withForce(true).exec();
            } catch (NotFoundException | InternalServerErrorException ignored) {

            }
        }
    }

    private static class NotEnoughDiskSpaceException extends RuntimeException {
        NotEnoughDiskSpaceException(String message) {
            super(message);
        }
    }
}

class LogContainerCallback extends LogContainerResultCallback {
    private final StringBuffer log = new StringBuffer();

    @Override
    public void onNext(Frame frame) {
        log.append(new String(frame.getPayload()));
        super.onNext(frame);
    }

    @Override
    public String toString() {
        return log.toString();
    }
}
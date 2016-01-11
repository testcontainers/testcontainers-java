package org.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.InternalServerErrorException;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import lombok.Synchronized;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.testcontainers.utility.DockerMachineClient;

import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Singleton class that provides initialized Docker clients.
 *
 * The correct client configuration to use will be determined on first use, and cached thereafter.
 */
public class DockerClientFactory {

    private static DockerClientFactory instance;
    private static final Logger LOGGER = getLogger(DockerClientFactory.class);

    // Cached client configuration
    private DockerClientConfig config;

    /**
     * Private constructor
     */
    private DockerClientFactory() {
        try {
            String version = client().versionCmd().exec().getVersion();
            checkVersion(version);

            checkDiskSpace();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to prepare Docker client", e);
        }
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
     * @return a new initialized Docker client
     */
    @Synchronized
    public DockerClient client() {
        if (config == null) {
            config = determineConfiguration();
        }

        DockerClient client = DockerClientBuilder.getInstance(config).build();

        // Ping, to fail fast if our docker environment has gone away
        client.pingCmd().exec();

        return client;
    }

    /**
     * Determine the right DockerClientConfig to use for building clients by trial-and-error.
     *
     * @return  a working DockerClientConfig, as determined by successful execution of a ping command
     */
    private DockerClientConfig determineConfiguration() {

        // Try using environment variables
        try {
            LOGGER.info("Checking environment for docker settings");

            DockerClientConfig candidateConfig = DockerClientConfig.createDefaultConfigBuilder().build();
            DockerClient client = DockerClientBuilder.getInstance(candidateConfig).build();
            client.pingCmd().exec();

            LOGGER.info("Found docker client settings from environment");
            LOGGER.info("Docker host IP address is {}", dockerHostIpAddress(candidateConfig));

            return candidateConfig;
        } catch (Exception e) {
            LOGGER.info("Could not initialize docker settings using environment variables", e.getMessage());
        }

        // Try using Docker machine
        try {
            LOGGER.info("Checking for presence of docker-machine");

            boolean installed = DockerMachineClient.instance().isInstalled();

            checkArgument(installed, "docker-machine must be installed for use on OS X");

            Optional<String> machineNameOptional = DockerMachineClient.instance().getDefaultMachine();

            if (machineNameOptional.isPresent()) {
                String machineName = machineNameOptional.get();

                LOGGER.info("Found docker-machine, and will use first machine defined ({})", machineName);

                DockerMachineClient.instance().ensureMachineRunning(machineName);

                String dockerDaemonIpAddress = DockerMachineClient.instance().getDockerDaemonIpAddress(machineName);

                LOGGER.info("Docker daemon IP address for docker machine {} is {}", machineName, dockerDaemonIpAddress);

                DockerClientConfig candidateConfig = DockerClientConfig
                        .createDefaultConfigBuilder()
                        .withUri("https://" + dockerDaemonIpAddress + ":2376")
                        .withDockerCertPath(Paths.get(System.getProperty("user.home") + "/.docker/machine/certs/").toString())
                        .build();
                DockerClient client = DockerClientBuilder.getInstance(candidateConfig).build();

                // If the docker-machine VM has started, the docker daemon may still not be ready. Retry pinging until it works.
                Unreliables.retryUntilSuccess(30, TimeUnit.SECONDS, () -> {
                    client.pingCmd().exec();
                    return true;
                });
                return candidateConfig;
            }
        } catch (Exception e) {
            LOGGER.debug("Could not initialize docker settings using docker machine", e.getMessage());
        }

        throw new IllegalStateException("Could not find a suitable docker instance - is DOCKER_HOST defined and pointing to a running Docker daemon?");
    }

    /**
     *
     * @param config docker client configuration to extract the host IP address from
     * @return the IP address of the host running Docker
     */
    private String dockerHostIpAddress(DockerClientConfig config) {
        return config.getUri().getHost();
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

    /**
     * Check whether this docker installation is likely to have disk space problems
     */
    private void checkDiskSpace() {
        DockerClient client = client();

        List<Image> images = client.listImagesCmd().exec();
        if (!images.stream().anyMatch(it -> asList(it.getRepoTags()).contains("alpine:3.2"))) {
            PullImageResultCallback callback = client.pullImageCmd("alpine:3.2").exec(new PullImageResultCallback());
            callback.awaitSuccess();
        }

        CreateContainerResponse createContainerResponse = client.createContainerCmd("alpine:3.2").withCmd("df").exec();
        String id = createContainerResponse.getId();

        client.startContainerCmd(id).exec();

        client.waitContainerCmd(id).exec();

        LogContainerResultCallback callback = client.logContainerCmd(id).withStdOut().exec(new LogContainerCallback());
        try {
            callback.awaitCompletion();
            String logResults = callback.toString();

            int use = 0;
            String[] lines = logResults.split("\n");
            for (String line : lines) {
                String[] fields = line.split("\\s+");
                if (fields[5].equals("/")) {
                    use = Integer.valueOf(fields[4].replace("%", ""));
                }
            }

            LOGGER.info("Disk utilization in Docker environment is {}%", use);

            if (use > 90) {
                LOGGER.warn("Disk utilization Docker environment is over 90% - execution is unlikely to succeed so will be aborted.");
                throw new IllegalStateException("Not enough disk space in Docker environment");
            }
        } catch (InterruptedException e) {
            LOGGER.info("Encountered error while checking disk space", e);
            throw new IllegalStateException(e);
        } finally {
            try {
                client.removeContainerCmd(id).withRemoveVolumes(true).withForce(true).exec();
            } catch (InternalServerErrorException ignored) {

            }
        }


    }
}

class LogContainerCallback extends LogContainerResultCallback {
    protected final StringBuffer log = new StringBuffer();

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
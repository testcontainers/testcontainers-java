package org.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import org.slf4j.Logger;

import java.nio.file.Paths;

import static org.slf4j.LoggerFactory.getLogger;
import static org.testcontainers.utility.CommandLine.runShellCommand;

/**
 * Singleton class that provides an instance of a docker client.
 */
public class SingletonDockerClient {

    private static SingletonDockerClient instance;
    private final DockerClient client;

    private static final Logger LOGGER = getLogger(SingletonDockerClient.class);
    private DockerClientConfig config;

    /**
     * Private constructor
     */
    private SingletonDockerClient() {
        try {
            client = createClient();

            String version = client.versionCmd().exec().getVersion();
            checkVersion(version);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Docker client", e);
        }
    }

    private DockerClient createClient() {

        // Try using environment variables
        try {
            config = DockerClientConfig.createDefaultConfigBuilder().build();
            DockerClient client = DockerClientBuilder.getInstance(config).build();
            client.pingCmd().exec();

            LOGGER.debug("Found docker client settings from environment");
            LOGGER.debug("Docker host IP address is {}", dockerHostIpAddress());

            return client;
        } catch (Exception e) {
            LOGGER.debug("Could not initialize docker settings using environment variables", e.getMessage());
        }

        // Try using Docker machine
        try {
            String ls = runShellCommand("docker-machine", "ls", "-q");
            String[] machineNames = ls.split("\n");
            if (machineNames.length > 0) {
                String machineName = machineNames[0];

                LOGGER.debug("Found docker-machine, and will use first machine defined ({})", machineName);

                String dockerHostIpAddress = runShellCommand("docker-machine", "ip", machineName);

                LOGGER.debug("Docker-machine IP address for {} is {}", machineName, dockerHostIpAddress);

                config = DockerClientConfig
                        .createDefaultConfigBuilder()
                        .withUri("https://" + dockerHostIpAddress + ":2376")
                        .withDockerCertPath(Paths.get(System.getProperty("user.home") + "/.docker/machine/certs/").toString())
                        .build();
                DockerClient client = DockerClientBuilder.getInstance(config).build();
                client.pingCmd().exec();
                return client;
            }
        } catch (Exception e) {
            LOGGER.debug("Could not initialize docker settings using docker machine", e.getMessage());
        }

        throw new IllegalStateException("Could not find a suitable docker instance - is DOCKER_HOST defined and pointing to a running Docker daemon?");
    }

    /**
     * Obtain an instance of the SingletonDockerClient wrapper.
     * @return the singleton instance of SingletonDockerClient
     */
    public synchronized static SingletonDockerClient instance() {
        if (instance == null) {
            instance = new SingletonDockerClient();
        }

        return instance;
    }

    /**
     * @return an initialized Docker client
     */
    public DockerClient client() {
        return client;
    }

    /**
     * @return the IP address of the host running Docker
     */
    public String dockerHostIpAddress() {
        return config.getUri().getHost();
    }

    private void checkVersion(String version) {
        String[] splitVersion = version.split("\\.");
        if (Integer.valueOf(splitVersion[0]) <= 1 && Integer.valueOf(splitVersion[1]) < 6) {
            throw new IllegalStateException("Docker version 1.6.0+ is required, but version " + version + " was found");
        }
    }
}

package org.testcontainers;

import com.spotify.docker.client.DefaultDockerClient;
import com.spotify.docker.client.DockerCertificates;
import com.spotify.docker.client.DockerClient;
import org.slf4j.Logger;

import java.nio.file.Paths;

import static org.slf4j.LoggerFactory.getLogger;
import static org.testcontainers.utility.CommandLine.runShellCommand;

/**
 * Created by rnorth on 09/08/2015.
 */
public class SingletonDockerClient {

    private static SingletonDockerClient instance;
    private final DockerClient client;
    private String dockerHostIpAddress;

    private static final Logger LOGGER = getLogger(SingletonDockerClient.class);

    private SingletonDockerClient() {
        try {
            client = createClient();

            String version = client.version().version();
            checkVersion(version);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create Docker client", e);
        }
    }

    private DockerClient createClient() {

        // Try using environment variables
        try {
            DockerClient client = DefaultDockerClient.fromEnv().build();
            client.ping();

            LOGGER.debug("Found docker client settings from environment");
            dockerHostIpAddress = client.getHost();
            LOGGER.debug("Docker host IP address is {}", dockerHostIpAddress);

            return client;
        } catch (Exception e) {
            LOGGER.debug("Could not initialize docker settings using environment variables", e);
        }

        // Try using Docker machine
        try {
            String ls = runShellCommand("docker-machine", "ls", "-q");
            String[] machineNames = ls.split("\n");
            if (machineNames.length > 0) {
                String machineName = machineNames[0];

                LOGGER.debug("Found docker-machine, and will use first machine defined ({})", machineName);

                dockerHostIpAddress = runShellCommand("docker-machine", "ip", machineName);

                LOGGER.debug("Docker-machine IP address for {} is {}", machineName, dockerHostIpAddress);

                return DefaultDockerClient.builder().uri("https://" + dockerHostIpAddress + ":2376")
                        .dockerCertificates(new DockerCertificates(Paths.get(System.getProperty("user.home") + "/.docker/machine/certs/")))
                        .build();
            }
        } catch (Exception e) {
            LOGGER.debug("Could not initialize docker settings using docker machine", e);
        }

        throw new IllegalStateException();
    }

    public synchronized static SingletonDockerClient instance() {
        if (instance == null) {
            instance = new SingletonDockerClient();
        }

        return instance;
    }

    public DockerClient client() {
        return client;
    }

    public String dockerHostIpAddress() {
        return dockerHostIpAddress;
    }

    private void checkVersion(String version) {
        String[] splitVersion = version.split("\\.");
        if (Integer.valueOf(splitVersion[0]) <= 1 && Integer.valueOf(splitVersion[1]) < 6) {
            throw new IllegalStateException("Docker version 1.6.0+ is required, but version " + version + " was found");
        }
    }
}

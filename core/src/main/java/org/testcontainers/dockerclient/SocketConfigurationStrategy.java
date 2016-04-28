package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

/**
 * Attempt to configure docker using a socket.
 *
 * @author richardnorth
 * @author pcornish
 */
public class SocketConfigurationStrategy implements DockerConfigurationStrategy {
    /**
     * Location of the socket.
     */
    private final String socketLocation;

    /**
     * Human readable description of the socket.
     */
    private final String socketDescription;

    /**
     * @param socketLocation    the location of the socket, such as {@literal http://path/to/socket}
     * @param socketDescription the human readable description of the socket
     */
    public SocketConfigurationStrategy(String socketLocation, String socketDescription) {
        this.socketLocation = socketLocation;
        this.socketDescription = socketDescription;
    }

    @Override
    public DockerClientConfig provideConfiguration() throws InvalidConfigurationException {
        final DockerClientConfig config = new DockerClientConfig.DockerClientConfigBuilder().withUri(socketLocation).build();
        final DockerClient client = DockerClientBuilder.getInstance(config).build();

        try {
            client.pingCmd().exec();
        } catch (Exception e) {
            throw new InvalidConfigurationException("ping failed", e);
        }

        LOGGER.info("Accessing docker with {} socket", socketDescription);
        return config;
    }

    @Override
    public String getDescription() {
        return socketDescription + " socket (" + socketLocation + ")";
    }
}

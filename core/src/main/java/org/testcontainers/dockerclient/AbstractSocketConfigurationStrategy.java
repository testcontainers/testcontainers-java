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
public abstract class AbstractSocketConfigurationStrategy implements DockerConfigurationStrategy {
    @Override
    public DockerClientConfig provideConfiguration() throws InvalidConfigurationException {
        final DockerClientConfig config = new DockerClientConfig.DockerClientConfigBuilder().withUri(getSocketLocation()).build();
        final DockerClient client = DockerClientBuilder.getInstance(config).build();

        try {
            client.pingCmd().exec();
        } catch (Exception e) {
            throw new InvalidConfigurationException("ping failed", e);
        }

        LOGGER.info("Accessing docker with {} socket", getSocketDescription());
        return config;
    }

    @Override
    public String getDescription() {
        return getSocketDescription() + " socket (" + getSocketLocation() + ")";
    }

    /**
     * @return the location of the socket, such as {@literal http://path/to/socket}
     */
    protected abstract String getSocketLocation();

    /**
     * @return the human readable description of the socket
     */
    protected abstract String getSocketDescription();
}

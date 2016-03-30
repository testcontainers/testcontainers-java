package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

public class UnixSocketConfigurationStrategy implements DockerConfigurationStrategy {

    public static final String SOCKET_LOCATION = "unix:///var/run/docker.sock";

    @Override
    public DockerClientConfig provideConfiguration()
            throws InvalidConfigurationException {
        DockerClientConfig config = new DockerClientConfig.DockerClientConfigBuilder().withUri(SOCKET_LOCATION).build();
        DockerClient client = DockerClientBuilder.getInstance(config).build();

        try {
            client.pingCmd().exec();
        } catch (Exception e) {
            throw new InvalidConfigurationException("ping failed", e);
        }

        LOGGER.info("Accessing docker with local Unix socket");
        return config;
    }

    @Override
    public String getDescription() {
        return "local Unix socket (" + SOCKET_LOCATION + ")";
    }

}

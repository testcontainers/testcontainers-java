package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

public class UnixSocketConfigurationStrategy implements DockerConfigurationStrategy {

    @Override
    public DockerClientConfig provideConfiguration()
            throws InvalidConfigurationException {
        DockerClientConfig config = new DockerClientConfig.DockerClientConfigBuilder().withUri("unix:///var/run/docker.sock").build();
        DockerClient client = DockerClientBuilder.getInstance(config).build();

        try {
            client.pingCmd().exec();
        } catch (Exception e) {
            throw new InvalidConfigurationException("ping failed");
        }

        LOGGER.info("Access docker with unix local socker");
        return config;
    }

    @Override
    public String getDescription() {
        return "unix socket docker access";
    }

}

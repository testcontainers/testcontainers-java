package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

/**
 * Created by rnorth on 13/01/2016.
 */
public class EnvironmentVariableConfigurationStrategy implements DockerConfigurationStrategy {

    private DockerClientConfig config = DockerClientConfig.createDefaultConfigBuilder().build();

    @Override
    public DockerClientConfig provideConfiguration() throws InvalidConfigurationException {
        // Try using environment variables
        DockerClientConfig candidateConfig = config;
        DockerClient client = DockerClientBuilder.getInstance(candidateConfig).build();

        try {
            client.pingCmd().exec();
        } catch (Exception e) {
            throw new InvalidConfigurationException("ping failed");
        }

        LOGGER.info("Found docker client settings from environment");
        LOGGER.info("Docker host IP address is {}", DockerClientConfigUtils.getDockerHostIpAddress(candidateConfig));

        return candidateConfig;
    }

    @Override
    public String getDescription() {
        return "Environment variables, system properties and defaults: " + config.toString();
    }
}

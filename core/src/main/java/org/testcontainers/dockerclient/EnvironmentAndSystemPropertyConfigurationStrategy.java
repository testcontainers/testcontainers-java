package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;

/**
 * Use environment variables and system properties (as supported by the underlying DockerClient DefaultConfigBuilder)
 * to try and locate a docker environment.
 */
public class EnvironmentAndSystemPropertyConfigurationStrategy implements DockerConfigurationStrategy {

    private DockerClientConfig config = null;

    @Override
    public DockerClientConfig provideConfiguration(DockerCmdExecFactory cmdExecFactory) throws InvalidConfigurationException {

        try {
            // Try using environment variables
            config = DockerClientConfig.createDefaultConfigBuilder().build();
            DockerClient client = DockerClientBuilder
                    .getInstance(config)
                    .withDockerCmdExecFactory(cmdExecFactory)
                    .build();

            client.pingCmd().exec();
        } catch (Exception e) {
            throw new InvalidConfigurationException("ping failed");
        }

        LOGGER.info("Found docker client settings from environment");
        LOGGER.info("Docker host IP address is {}", DockerClientConfigUtils.getDockerHostIpAddress(config));

        return config;
    }

    @Override
    public String getDescription() {
        return "Environment variables, system properties and defaults. Resolved: \n" + stringRepresentation(config);
    }

    private String stringRepresentation(DockerClientConfig config) {

        if (config == null) {
            return "";
        }

        return  "    dockerHost=" + config.getDockerHost() + "\n" +
                "    dockerCertPath='" + config.getDockerCertPath() + "'\n" +
                "    dockerTlsVerify='" + config.getDockerTlsVerify() + "'\n" +
                "    apiVersion='" + config.getApiVersion() + "'\n" +
                "    registryUrl='" + config.getRegistryUrl() + "'\n" +
                "    registryUsername='" + config.getRegistryUsername() + "'\n" +
                "    registryPassword='" + config.getRegistryPassword() + "'\n" +
                "    registryEmail='" + config.getRegistryEmail() + "'\n" +
                "    dockerConfig='" + config.getDockerConfig() + "'\n";
    }
}

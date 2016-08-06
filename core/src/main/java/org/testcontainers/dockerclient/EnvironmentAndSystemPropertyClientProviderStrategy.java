package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DockerClientConfig;

/**
 * Use environment variables and system properties (as supported by the underlying DockerClient DefaultConfigBuilder)
 * to try and locate a docker environment.
 */
public class EnvironmentAndSystemPropertyClientProviderStrategy extends DockerClientProviderStrategy {

    @Override
    public void test() throws InvalidConfigurationException {

        try {
            // Try using environment variables
            config = DockerClientConfig.createDefaultConfigBuilder().build();
            client = getClientForConfig(config);

            ping(client, 1);
        } catch (Exception e) {
            throw new InvalidConfigurationException("ping failed");
        }

        LOGGER.info("Found docker client settings from environment");
        LOGGER.info("Docker host IP address is {}", DockerClientConfigUtils.getDockerHostIpAddress(config));
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
                "    apiVersion='" + config.getApiVersion() + "'\n" +
                "    registryUrl='" + config.getRegistryUrl() + "'\n" +
                "    registryUsername='" + config.getRegistryUsername() + "'\n" +
                "    registryPassword='" + config.getRegistryPassword() + "'\n" +
                "    registryEmail='" + config.getRegistryEmail() + "'\n" +
                "    dockerConfig='" + config.toString() + "'\n";
    }
}

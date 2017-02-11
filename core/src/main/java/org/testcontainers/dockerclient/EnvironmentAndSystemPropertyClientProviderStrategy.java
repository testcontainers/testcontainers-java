package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;

/**
 * Use environment variables and system properties (as supported by the underlying DockerClient DefaultConfigBuilder)
 * to try and locate a docker environment.
 */
public class EnvironmentAndSystemPropertyClientProviderStrategy extends DockerClientProviderStrategy {
    private static final String PING_TIMEOUT_DEFAULT = "10";
    private static final String PING_TIMEOUT_PROPERTY_NAME = "testcontainers.environmentprovider.timeout";

    @Override
    public void test() throws InvalidConfigurationException {

        try {
            // Try using environment variables
            config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
            client = getClientForConfig(config);

            final int timeout = Integer.parseInt(System.getProperty(PING_TIMEOUT_PROPERTY_NAME, PING_TIMEOUT_DEFAULT));
            ping(client, timeout);
        } catch (Exception | UnsatisfiedLinkError e) {
            LOGGER.error("ping failed with configuration {} due to {}", getDescription(), e.toString(), e);
            throw new InvalidConfigurationException("ping failed");
        }

        LOGGER.info("Found docker client settings from environment");
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

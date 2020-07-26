package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;

/**
 * Use environment variables and system properties (as supported by the underlying DockerClient DefaultConfigBuilder)
 * to try and locate a docker environment.
 *
 * @deprecated this class is used by the SPI and should not be used directly
 */
@Deprecated
public final class EnvironmentAndSystemPropertyClientProviderStrategy extends DockerClientProviderStrategy {

    public static final int PRIORITY = 100;

    // Try using environment variables
    private final DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();

    @Override
    protected boolean isApplicable() {
        return System.getenv("DOCKER_HOST") != null;
    }

    @Override
    public TransportConfig getTransportConfig() {
        return TransportConfig.builder()
            .dockerHost(dockerClientConfig.getDockerHost())
            .sslConfig(dockerClientConfig.getSSLConfig())
            .build();
    }

    @Override
    protected int getPriority() {
        return PRIORITY;
    }

    @Override
    public String getDescription() {
        return "Environment variables, system properties and defaults. Resolved dockerHost=" + dockerClientConfig.getDockerHost();
    }
}

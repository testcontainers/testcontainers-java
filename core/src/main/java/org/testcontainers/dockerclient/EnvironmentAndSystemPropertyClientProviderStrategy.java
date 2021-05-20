package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.Optional;

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
    private final DockerClientConfig dockerClientConfig;

    public EnvironmentAndSystemPropertyClientProviderStrategy() {
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();

        getUserProperty("docker.host").ifPresent(configBuilder::withDockerHost);
        getUserProperty("docker.tls.verify").ifPresent(configBuilder::withDockerTlsVerify);
        getUserProperty("docker.cert.path").ifPresent(configBuilder::withDockerCertPath);

        dockerClientConfig = configBuilder.build();
    }

    private Optional<String> getUserProperty(final String name) {
        return Optional.ofNullable(TestcontainersConfiguration.getInstance().getEnvVarOrUserProperty(name, null));
    }

    @Override
    protected boolean isApplicable() {
        return System.getenv("DOCKER_HOST") != null || getUserProperty("docker.host").isPresent();
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

package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.Optional;

/**
 * Use environment variables and system properties (as supported by the underlying DockerClient DefaultConfigBuilder)
 * to try and locate a docker environment.
 * <p>
 * Resolution order is:
 * <ol>
 *     <li>DOCKER_HOST env var</li>
 *     <li>docker.host in ~/.testcontainers.properties</li>
 * </ol>
 *
 * @deprecated this class is used by the SPI and should not be used directly
 */
@Deprecated
public final class EnvironmentAndSystemPropertyClientProviderStrategy extends DockerClientProviderStrategy {

    public static final int PRIORITY = 100;

    private final DockerClientConfig dockerClientConfig;

    public EnvironmentAndSystemPropertyClientProviderStrategy() {
        // use docker-java defaults if present, overridden if our own configuration is set
        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder();

        getSetting("docker.host").ifPresent(configBuilder::withDockerHost);
        getSetting("docker.tls.verify").ifPresent(configBuilder::withDockerTlsVerify);
        getSetting("docker.cert.path").ifPresent(configBuilder::withDockerCertPath);

        dockerClientConfig = configBuilder.build();
    }

    private Optional<String> getSetting(final String name) {
        return Optional.ofNullable(TestcontainersConfiguration.getInstance().getEnvVarOrUserProperty(name, null));
    }

    @Override
    protected boolean isApplicable() {
        return getSetting("docker.host").isPresent();
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

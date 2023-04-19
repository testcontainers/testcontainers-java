package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import lombok.Getter;
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

    @Getter
    private final boolean applicable;

    public EnvironmentAndSystemPropertyClientProviderStrategy() {
        // use docker-java defaults if present, overridden if our own configuration is set
        this(DefaultDockerClientConfig.createDefaultConfigBuilder());
    }

    EnvironmentAndSystemPropertyClientProviderStrategy(DefaultDockerClientConfig.Builder configBuilder) {
        boolean applicable = false;
        String dockerConfigSource = TestcontainersConfiguration
            .getInstance()
            .getEnvVarOrProperty("dockerconfig.source", "auto");

        switch (dockerConfigSource) {
            case "auto":
                Optional<String> dockerHost = getSetting("docker.host");
                dockerHost.ifPresent(configBuilder::withDockerHost);
                applicable = dockerHost.isPresent();
                getSetting("docker.tls.verify").ifPresent(configBuilder::withDockerTlsVerify);
                getSetting("docker.cert.path").ifPresent(configBuilder::withDockerCertPath);
                break;
            case "autoIgnoringUserProperties":
                applicable = configBuilder.isDockerHostSetExplicitly();
                break;
            default:
                throw new InvalidConfigurationException("Invalid value for dockerconfig.source: " + dockerConfigSource);
        }

        dockerClientConfig = configBuilder.build();
        // DefaultDockerClientConfig.Builder is often able to read a docker host from the current context file.
        // If it does not find such a thing and there's also no explicit host setting, it will set the host to the default
        // Unix or Windows socket location. So if we find something other than those defaults, we'll use it; otherwise,
        // we'll let other strategies kick in (including UnixSocketClientProviderStrategy and NpipeSocketClientProviderStrategy
        // themselves).
        applicable =
            applicable ||
            (
                !dockerClientConfig.getDockerHost().equals(UnixSocketClientProviderStrategy.SOCKET_LOCATION) &&
                !dockerClientConfig.getDockerHost().equals(NpipeSocketClientProviderStrategy.SOCKET_LOCATION)
            );
        this.applicable = applicable;
    }

    private Optional<String> getSetting(final String name) {
        return Optional.ofNullable(TestcontainersConfiguration.getInstance().getEnvVarOrUserProperty(name, null));
    }

    @Override
    public TransportConfig getTransportConfig() {
        return TransportConfig
            .builder()
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
        return (
            "Environment variables, system properties and defaults. Resolved dockerHost=" +
            dockerClientConfig.getDockerHost()
        );
    }

    @Override
    protected boolean isPersistable() {
        return false;
    }
}

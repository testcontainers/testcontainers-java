package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.Optional;

/**
 * Use <code>tc.host</code> in <code>~/.testcontainers.properties</code>
 * to try and locate a docker environment.
 *
 * @deprecated this class is used by the SPI and should not be used directly
 */
@Deprecated
public final class TestcontainersHostPropertyClientStrategy extends DockerClientProviderStrategy {

    public static final int PRIORITY = EnvironmentAndSystemPropertyClientProviderStrategy.PRIORITY - 10;

    private final DockerClientConfig dockerClientConfig;

    public TestcontainersHostPropertyClientStrategy() {
        this(DefaultDockerClientConfig.createDefaultConfigBuilder());
    }

    public TestcontainersHostPropertyClientStrategy(DefaultDockerClientConfig.Builder configBuilder) {
        Optional<String> tcHost = Optional.ofNullable(
            TestcontainersConfiguration.getInstance().getEnvVarOrUserProperty("tc.host", null)
        );

        tcHost.ifPresent(configBuilder::withDockerHost);
        this.dockerClientConfig = configBuilder.build();
    }

    @Override
    public String getDescription() {
        return "Testcontainers Host Strategy with tc.host=" + this.dockerClientConfig.getDockerHost();
    }

    @Override
    public TransportConfig getTransportConfig() throws InvalidConfigurationException {
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
    protected boolean isPersistable() {
        return false;
    }
}

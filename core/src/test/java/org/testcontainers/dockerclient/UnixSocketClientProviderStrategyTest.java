package org.testcontainers.dockerclient;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class UnixSocketClientProviderStrategyTest {

    @Test
    void shouldRespectDockerHostEnvVar() {
        String customSocketPath = "/tmp/test-socket-path";
        String envVarValue = "unix://" + customSocketPath;

        UnixSocketClientProviderStrategy strategy = new UnixSocketClientProviderStrategy() {
            @Override
            protected String getDockerHostEnv() {
                return envVarValue;
            }
        };

        Throwable thrown = catchThrowable(strategy::getTransportConfig);

        assertThat(thrown)
            .as("Strategy should use path from DOCKER_HOST env var")
            .hasMessageContaining(customSocketPath);
    }
}

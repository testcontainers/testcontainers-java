package org.testcontainers.dockerclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testcontainers.utility.MockTestcontainersConfigurationExtension;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

@ExtendWith(MockTestcontainersConfigurationExtension.class)
class TestcontainersHostPropertyClientProviderStrategyTest {

    @Test
    void tcHostPropertyIsProvided() {
        Mockito
            .doReturn("tcp://127.0.0.1:9000")
            .when(TestcontainersConfiguration.getInstance())
            .getUserProperty(eq("tc.host"), isNull());

        TestcontainersHostPropertyClientProviderStrategy strategy = new TestcontainersHostPropertyClientProviderStrategy();

        assertThat(strategy.isApplicable()).isTrue();
        TransportConfig transportConfig = strategy.getTransportConfig();
        assertThat(transportConfig.getDockerHost().toString()).isEqualTo("tcp://127.0.0.1:9000");
    }

    @Test
    void tcHostPropertyIsNotProvided() {
        Mockito.doReturn(null).when(TestcontainersConfiguration.getInstance()).getUserProperty(eq("tc.host"), isNull());

        TestcontainersHostPropertyClientProviderStrategy strategy = new TestcontainersHostPropertyClientProviderStrategy();

        assertThat(strategy.isApplicable()).isFalse();
    }
}

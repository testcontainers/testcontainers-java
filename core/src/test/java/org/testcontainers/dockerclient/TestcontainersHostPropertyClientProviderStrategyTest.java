package org.testcontainers.dockerclient;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.testcontainers.utility.MockTestcontainersConfigurationRule;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

@RunWith(MockitoJUnitRunner.class)
public class TestcontainersHostPropertyClientProviderStrategyTest {

    @Rule
    public MockTestcontainersConfigurationRule mockConfig = new MockTestcontainersConfigurationRule();

    private URI defaultDockerHost;

    @Test
    public void tcHostPropertyIsProvided() {
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
    public void tcHostPropertyIsNotProvided() {
        Mockito.doReturn(null).when(TestcontainersConfiguration.getInstance()).getUserProperty(eq("tc.host"), isNull());

        TestcontainersHostPropertyClientProviderStrategy strategy = new TestcontainersHostPropertyClientProviderStrategy();

        assertThat(strategy.isApplicable()).isFalse();
    }
}

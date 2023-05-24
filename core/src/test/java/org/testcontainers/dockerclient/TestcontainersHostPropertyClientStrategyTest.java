package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
public class TestcontainersHostPropertyClientStrategyTest {

    @Rule
    public MockTestcontainersConfigurationRule mockConfig = new MockTestcontainersConfigurationRule();

    private URI defaultDockerHost;

    private com.github.dockerjava.core.SSLConfig defaultSSLConfig;

    @Before
    public void checkEnvironmentClear() {
        // If docker-java picks up non-default settings from the environment, our test needs to know to expect those
        DefaultDockerClientConfig defaultConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        this.defaultDockerHost = defaultConfig.getDockerHost();
        this.defaultSSLConfig = defaultConfig.getSSLConfig();
    }

    @Test
    public void tcHostPropertyIsProvided() {
        Mockito
            .doReturn("tcp://127.0.0.1:9000")
            .when(TestcontainersConfiguration.getInstance())
            .getEnvVarOrUserProperty(eq("tc.host"), isNull());

        TestcontainersHostPropertyClientStrategy strategy = new TestcontainersHostPropertyClientStrategy();

        TransportConfig transportConfig = strategy.getTransportConfig();
        assertThat(transportConfig.getDockerHost().toString()).isEqualTo("tcp://127.0.0.1:9000");
        assertThat(transportConfig.getSslConfig()).isEqualTo(this.defaultSSLConfig);
    }

    @Test
    public void tcHostPropertyIsNotProvided() {
        Mockito
            .doReturn(null)
            .when(TestcontainersConfiguration.getInstance())
            .getEnvVarOrUserProperty(eq("tc.host"), isNull());

        TestcontainersHostPropertyClientStrategy strategy = new TestcontainersHostPropertyClientStrategy();

        TransportConfig transportConfig = strategy.getTransportConfig();
        assertThat(transportConfig.getDockerHost()).isEqualTo(this.defaultDockerHost);
        assertThat(transportConfig.getSslConfig()).isEqualTo(this.defaultSSLConfig);
    }
}

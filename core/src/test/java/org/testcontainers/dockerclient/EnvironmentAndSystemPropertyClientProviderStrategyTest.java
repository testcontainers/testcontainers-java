package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.LocalDirectorySSLConfig;
import com.github.dockerjava.transport.SSLConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.testcontainers.utility.MockTestcontainersConfigurationRule;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

/**
 * Test that we can use Testcontainers configuration file to override settings. We assume that docker-java has test
 * coverage for detection of environment variables (e.g. DOCKER_HOST) and its own properties config file.
 */
@RunWith(MockitoJUnitRunner.class)
public class EnvironmentAndSystemPropertyClientProviderStrategyTest {

    @Rule
    public MockTestcontainersConfigurationRule mockConfig = new MockTestcontainersConfigurationRule();
    private URI defaultDockerHost;
    private com.github.dockerjava.core.SSLConfig defaultSSLConfig;

    @Before
    public void checkEnvironmentClear() {
        // If docker-java picks up non-default settings from the environment, our test needs to know to expect those
        DefaultDockerClientConfig defaultConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        defaultDockerHost = defaultConfig.getDockerHost();
        defaultSSLConfig = defaultConfig.getSSLConfig();
    }

    @Test
    public void testWhenConfigAbsent() {
        Mockito.doReturn("auto").when(TestcontainersConfiguration.getInstance()).getEnvVarOrProperty(eq("dockerconfig.source"), anyString());
        Mockito.doReturn(null).when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.host"), isNull());
        Mockito.doReturn(null).when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.tls.verify"), isNull());
        Mockito.doReturn(null).when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.cert.path"), isNull());

        EnvironmentAndSystemPropertyClientProviderStrategy strategy = new EnvironmentAndSystemPropertyClientProviderStrategy();

        TransportConfig transportConfig = strategy.getTransportConfig();
        assertEquals(defaultDockerHost, transportConfig.getDockerHost());
        assertEquals(defaultSSLConfig, transportConfig.getSslConfig());
    }

    @Test
    public void testWhenDockerHostPresent() {
        Mockito.doReturn("auto").when(TestcontainersConfiguration.getInstance()).getEnvVarOrProperty(eq("dockerconfig.source"), anyString());
        Mockito.doReturn("tcp://1.2.3.4:2375").when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.host"), isNull());
        Mockito.doReturn(null).when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.tls.verify"), isNull());
        Mockito.doReturn(null).when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.cert.path"), isNull());

        EnvironmentAndSystemPropertyClientProviderStrategy strategy = new EnvironmentAndSystemPropertyClientProviderStrategy();

        TransportConfig transportConfig = strategy.getTransportConfig();
        assertEquals("tcp://1.2.3.4:2375", transportConfig.getDockerHost().toString());
        assertEquals(defaultSSLConfig, transportConfig.getSslConfig());
    }

    @Test
    public void testWhenDockerHostAndSSLConfigPresent() throws IOException {
        Path tempDir = Files.createTempDirectory("testcontainers-test");
        String tempDirPath = tempDir.toAbsolutePath().toString();

        Mockito.doReturn("auto").when(TestcontainersConfiguration.getInstance()).getEnvVarOrProperty(eq("dockerconfig.source"), anyString());
        Mockito.doReturn("tcp://1.2.3.4:2375").when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.host"), isNull());
        Mockito.doReturn("1").when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.tls.verify"), isNull());
        Mockito.doReturn(tempDirPath).when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.cert.path"), isNull());

        EnvironmentAndSystemPropertyClientProviderStrategy strategy = new EnvironmentAndSystemPropertyClientProviderStrategy();

        TransportConfig transportConfig = strategy.getTransportConfig();
        assertEquals("tcp://1.2.3.4:2375", transportConfig.getDockerHost().toString());

        SSLConfig sslConfig = transportConfig.getSslConfig();
        assertNotNull(sslConfig);
        assertTrue(sslConfig instanceof LocalDirectorySSLConfig);
        assertEquals(tempDirPath, ((LocalDirectorySSLConfig) sslConfig).getDockerCertPath());
    }

    @Test
    public void applicableWhenIgnoringUserPropertiesAndConfigured() {
        Mockito.doReturn("autoIgnoringUserProperties").when(TestcontainersConfiguration.getInstance()).getEnvVarOrProperty(eq("dockerconfig.source"), anyString());

        DefaultDockerClientConfig.Builder configBuilder = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("tcp://1.2.3.4:2375");

        EnvironmentAndSystemPropertyClientProviderStrategy strategy = new EnvironmentAndSystemPropertyClientProviderStrategy(configBuilder);

        assertTrue(strategy.isApplicable());
    }

    @Test
    public void notApplicableWhenIgnoringUserPropertiesAndNotConfigured() {
        Mockito.doReturn("autoIgnoringUserProperties").when(TestcontainersConfiguration.getInstance()).getEnvVarOrProperty(eq("dockerconfig.source"), anyString());

        EnvironmentAndSystemPropertyClientProviderStrategy strategy = new EnvironmentAndSystemPropertyClientProviderStrategy();

        assertFalse(strategy.isApplicable());
    }
}

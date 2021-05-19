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
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
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

    @Before
    public void checkEnvironmentClear() {
        // If docker-java will pick up non-default settings from the environment, testing will not be reliable - skip tests
        DefaultDockerClientConfig defaultConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        assumeThat(defaultConfig.getDockerHost().toASCIIString(), is("unix:///var/run/docker.sock"));
        assumeThat(defaultConfig.getSSLConfig(), is(nullValue()));
    }

    @Test
    public void testWhenConfigAbsent() {
        Mockito.doReturn(null).when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.host"), isNull());
        Mockito.doReturn(null).when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.tls.verify"), isNull());
        Mockito.doReturn(null).when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.cert.path"), isNull());

        EnvironmentAndSystemPropertyClientProviderStrategy strategy = new EnvironmentAndSystemPropertyClientProviderStrategy();

        TransportConfig transportConfig = strategy.getTransportConfig();
        assertEquals("unix:///var/run/docker.sock", transportConfig.getDockerHost().toString());
        assertNull(transportConfig.getSslConfig());

        String dockerHostIpAddress = strategy.getDockerHostIpAddress();
        assertEquals("localhost", dockerHostIpAddress);
    }

    @Test
    public void testWhenDockerHostPresent() {
        Mockito.doReturn("tcp://1.2.3.4:2375").when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.host"), isNull());
        Mockito.doReturn(null).when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.tls.verify"), isNull());
        Mockito.doReturn(null).when(TestcontainersConfiguration.getInstance()).getEnvVarOrUserProperty(eq("docker.cert.path"), isNull());

        EnvironmentAndSystemPropertyClientProviderStrategy strategy = new EnvironmentAndSystemPropertyClientProviderStrategy();

        TransportConfig transportConfig = strategy.getTransportConfig();
        assertEquals("tcp://1.2.3.4:2375", transportConfig.getDockerHost().toString());
        assertNull(transportConfig.getSslConfig());

        String dockerHostIpAddress = strategy.getDockerHostIpAddress();
        assertEquals("1.2.3.4", dockerHostIpAddress);
    }

    @Test
    public void testWhenDockerHostAndSSLConfigPresent() throws IOException {
        Path tempDir = Files.createTempDirectory("testcontainers-test");
        String tempDirPath = tempDir.toAbsolutePath().toString();

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

        String dockerHostIpAddress = strategy.getDockerHostIpAddress();
        assertEquals("1.2.3.4", dockerHostIpAddress);
    }
}

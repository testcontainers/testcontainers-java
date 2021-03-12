package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import org.assertj.core.api.Assumptions;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DockerClientConfigUtilsTest {

    DockerClient client = DockerClientFactory.lazyClient();

    @Test
    public void getDockerHostIpAddressShouldReturnLocalhostWhenUnixSocket() {
        Assumptions.assumeThat(DockerClientConfigUtils.IN_A_CONTAINER)
            .as("in a container")
            .isFalse();

        String actual = DockerClientProviderStrategy.resolveDockerHostIpAddress(client, URI.create("unix:///var/run/docker.sock"));
        assertEquals("localhost", actual);
    }

    @Test
    public void getDockerHostIpAddressShouldReturnDockerHostIpWhenHttpsUri() {
        String actual = DockerClientProviderStrategy.resolveDockerHostIpAddress(client, URI.create("http://12.23.34.45"));
        assertEquals("12.23.34.45", actual);
    }

    @Test
    public void getDockerHostIpAddressShouldReturnDockerHostIpWhenTcpUri() {
        String actual = DockerClientProviderStrategy.resolveDockerHostIpAddress(client, URI.create("tcp://12.23.34.45"));
        assertEquals("12.23.34.45", actual);
    }

    @Test
    public void getDockerHostIpAddressShouldReturnNullWhenUnsupportedUriScheme() {
        String actual = DockerClientProviderStrategy.resolveDockerHostIpAddress(client, URI.create("gopher://12.23.34.45"));
        assertNull(actual);
    }

    @Test(timeout = 5_000)
    public void getDefaultGateway() {
        assertNotNull(DockerClientConfigUtils.getDefaultGateway());
    }
}

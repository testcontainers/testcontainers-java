package org.testcontainers.dockerclient;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.github.dockerjava.core.DockerClientConfig;

public class DockerClientConfigUtilsTest {

    @Test
    public void getDockerHostIpAddressShouldReturnLocalhostWhenUnixSocket() {
        DockerClientConfig configuration = DockerClientConfig.createDefaultConfigBuilder().withUri("unix:///var/run/docker.sock").build();
        String actual = DockerClientConfigUtils.getDockerHostIpAddress(configuration);
        assertEquals("localhost", actual);
    }

    @Test
    public void getDockerHostIpAddressShouldReturnDockerHostIpWhenHttpUri() {
        DockerClientConfig configuration = DockerClientConfig.createDefaultConfigBuilder().withUri("http://12.23.34.45").build();
        String actual = DockerClientConfigUtils.getDockerHostIpAddress(configuration);
        assertEquals("12.23.34.45", actual);
    }

    @Test
    public void getDockerHostIpAddressShouldReturnDockerHostIpWhenHttpsUri() {
        DockerClientConfig configuration = DockerClientConfig.createDefaultConfigBuilder().withUri("https://12.23.34.45").build();
        String actual = DockerClientConfigUtils.getDockerHostIpAddress(configuration);
        assertEquals("12.23.34.45", actual);
    }
    
    @Test
    public void getDockerHostIpAddressShouldReturnDockerHostIpWhenTcpUri() {
        DockerClientConfig configuration = DockerClientConfig.createDefaultConfigBuilder().withUri("tcp://12.23.34.45").build();
        String actual = DockerClientConfigUtils.getDockerHostIpAddress(configuration);
        assertEquals("12.23.34.45", actual);
    }
    
    @Test
    public void getDockerHostIpAddressShouldReturnNullWhenUnsupportedUriScheme() {
        DockerClientConfig configuration = DockerClientConfig.createDefaultConfigBuilder().withUri("gopher://12.23.34.45").build();
        String actual = DockerClientConfigUtils.getDockerHostIpAddress(configuration);
        assertNull(actual);
    }
}

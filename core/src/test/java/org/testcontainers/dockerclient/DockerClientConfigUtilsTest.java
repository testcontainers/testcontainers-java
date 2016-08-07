package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DockerClientConfigUtilsTest {

    @Test
    public void getDockerHostIpAddressShouldReturnLocalhostWhenUnixSocket() {
        DockerClientConfig configuration = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("unix:///var/run/docker.sock")
                .withDockerTlsVerify(false) // TODO - check wrt. https://github.com/docker-java/docker-java/issues/588
                .build();
        String actual = DockerClientConfigUtils.getDockerHostIpAddress(configuration);
        assertEquals("localhost", actual);
    }

    @Test @Ignore
    public void getDockerHostIpAddressShouldReturnDockerHostIpWhenHttpUri() {
        DockerClientConfig configuration = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost("http://12.23.34.45").build();
        String actual = DockerClientConfigUtils.getDockerHostIpAddress(configuration);
        assertEquals("12.23.34.45", actual);
    }

    @Test @Ignore
    public void getDockerHostIpAddressShouldReturnDockerHostIpWhenHttpsUri() {
        DockerClientConfig configuration = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost("https://12.23.34.45").build();
        String actual = DockerClientConfigUtils.getDockerHostIpAddress(configuration);
        assertEquals("12.23.34.45", actual);
    }
    
    @Test
    public void getDockerHostIpAddressShouldReturnDockerHostIpWhenTcpUri() {
        DockerClientConfig configuration = DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost("tcp://12.23.34.45")
                .withDockerTlsVerify(false) // TODO - check wrt. https://github.com/docker-java/docker-java/issues/588
                .build();
        String actual = DockerClientConfigUtils.getDockerHostIpAddress(configuration);
        assertEquals("12.23.34.45", actual);
    }
    
    @Test @Ignore
    public void getDockerHostIpAddressShouldReturnNullWhenUnsupportedUriScheme() {
        DockerClientConfig configuration = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost("gopher://12.23.34.45").build();
        String actual = DockerClientConfigUtils.getDockerHostIpAddress(configuration);
        assertNull(actual);
    }
}

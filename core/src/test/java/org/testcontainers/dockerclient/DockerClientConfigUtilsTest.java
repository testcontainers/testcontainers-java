package org.testcontainers.dockerclient;

import com.github.dockerjava.core.DockerClientConfig;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DockerClientConfigUtilsTest {

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    public static final String CONTAINER_HOST_SYSPROP = "testcontainers.container.host";
    public static final String CONTAINER_HOST_ENVVAR = "TESTCONTAINERS_CONTAINER_HOST";

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

    @Test
    public void getDockerHostIpAddressShouldReturnOverrideWhenSystemPropertyIsSet() {
        DockerClientConfig configuration = DockerClientConfig.createDefaultConfigBuilder().withUri("unix:///var/run/docker.sock").build();
        try {
            System.setProperty(CONTAINER_HOST_SYSPROP, "12.23.34.45");
            String actual = DockerClientConfigUtils.getDockerHostIpAddress(configuration);
            assertEquals("12.23.34.45", actual);

        } finally {
            System.clearProperty(CONTAINER_HOST_SYSPROP);
        }
    }

    @Test
    public void getDockerHostIpAddressShouldReturnOverrideWhenEnvVarIsSet() {
        DockerClientConfig configuration = DockerClientConfig.createDefaultConfigBuilder().withUri("unix:///var/run/docker.sock").build();
        try {
            environmentVariables.set(CONTAINER_HOST_ENVVAR, "12.23.34.45");
            String actual = DockerClientConfigUtils.getDockerHostIpAddress(configuration);
            assertEquals("12.23.34.45", actual);

        } finally {
            environmentVariables.set(CONTAINER_HOST_ENVVAR, null);
        }
    }
}

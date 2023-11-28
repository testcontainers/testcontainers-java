package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import org.assertj.core.api.Assumptions;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerClientConfigUtilsTest {

    DockerClient client = DockerClientFactory.lazyClient();

    @Test
    public void getDockerHostIpAddressShouldReturnLocalhostWhenUnixSocket() {
        Assumptions.assumeThat(DockerClientConfigUtils.IN_A_CONTAINER).as("in a container").isFalse();

        String actual = DockerClientProviderStrategy.resolveDockerHostIpAddress(
            client,
            URI.create("unix:///var/run/docker.sock"),
            true
        );
        assertThat(actual).isEqualTo("localhost");
    }

    @Test
    public void getDockerHostIpAddressShouldReturnDockerHostIpWhenHttpsUri() {
        String actual = DockerClientProviderStrategy.resolveDockerHostIpAddress(
            client,
            URI.create("http://12.23.34.45"),
            true
        );
        assertThat(actual).isEqualTo("12.23.34.45");
    }

    @Test
    public void getDockerHostIpAddressShouldReturnDockerHostIpWhenTcpUri() {
        String actual = DockerClientProviderStrategy.resolveDockerHostIpAddress(
            client,
            URI.create("tcp://12.23.34.45"),
            true
        );
        assertThat(actual).isEqualTo("12.23.34.45");
    }

    @Test
    public void getDockerHostIpAddressShouldReturnNullWhenUnsupportedUriScheme() {
        String actual = DockerClientProviderStrategy.resolveDockerHostIpAddress(
            client,
            URI.create("gopher://12.23.34.45"),
            true
        );
        assertThat(actual).isNull();
    }

    @Test(timeout = 5_000)
    public void getDefaultGateway() {
        assertThat(DockerClientConfigUtils.getDefaultGateway()).isNotNull();
    }
}

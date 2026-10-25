package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.MockTestcontainersConfigurationExtension;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockTestcontainersConfigurationExtension.class)
class DockerClientConfigUtilsTest {

    DockerClient client = DockerClientFactory.lazyClient();

    @Test
    void getDockerHostIpAddressShouldReturnLocalhostWhenUnixSocket() {
        Assumptions.assumeThat(DockerClientConfigUtils.IN_A_CONTAINER).as("in a container").isFalse();

        String actual = DockerClientProviderStrategy.resolveDockerHostIpAddress(
            client,
            URI.create("unix:///var/run/docker.sock"),
            true
        );
        assertThat(actual).isEqualTo("localhost");
    }

    @Test
    void getDockerHostIpAddressShouldReturnDockerHostIpWhenHttpsUri() {
        String actual = DockerClientProviderStrategy.resolveDockerHostIpAddress(
            client,
            URI.create("http://12.23.34.45"),
            true
        );
        assertThat(actual).isEqualTo("12.23.34.45");
    }

    @Test
    void getDockerHostIpAddressShouldReturnDockerHostIpWhenTcpUri() {
        String actual = DockerClientProviderStrategy.resolveDockerHostIpAddress(
            client,
            URI.create("tcp://12.23.34.45"),
            true
        );
        assertThat(actual).isEqualTo("12.23.34.45");
    }

    @Test
    void getDockerHostIpAddressShouldReturnNullWhenUnsupportedUriScheme() {
        String actual = DockerClientProviderStrategy.resolveDockerHostIpAddress(
            client,
            URI.create("gopher://12.23.34.45"),
            true
        );
        assertThat(actual).isNull();
    }

    @Test
    @Timeout(5)
    void getDefaultGateway() {
        assertThat(DockerClientConfigUtils.getDefaultGateway()).isNotNull();
    }

    @Test
    @Timeout(5)
    void resolveDefaultGatewayShouldNotRunContainerWhenChecksAreDisabled() {
        Mockito.doReturn(true).when(TestcontainersConfiguration.getInstance()).isDisableChecks();

        assertThat(DockerClientConfigUtils.resolveDefaultGateway()).isEmpty();
    }
}

package org.testcontainers.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

@ExtendWith(MockTestcontainersConfigurationExtension.class)
class RyukContainerTest {

    @Test
    void shouldNotUseCustomNetworkByDefault() {
        assertThat(new RyukContainer().getNetworkMode()).as("no custom network by default").isNull();
    }

    @Test
    void shouldApplyConfiguredNetworkBeforeStart() {
        Mockito.doReturn("custom-network").when(TestcontainersConfiguration.getInstance()).getRyukNetwork();

        assertThat(new RyukContainer().getNetworkMode())
            .as("configured network is applied before the container is started")
            .isEqualTo("custom-network");
    }

    @Test
    void shouldExposeDefaultPortOnNonHostNetworks() {
        RyukContainer container = new RyukContainer();

        assertThat(container.getExposedPorts())
            .as("the Ryuk port is exposed on networks where Docker publishes ports")
            .containsExactly(8080);
        assertThat(container.getEnv())
            .as("Ryuk listens on its default port unless overridden")
            .noneMatch(it -> it.startsWith("RYUK_PORT="));
    }

    @Test
    void shouldUseRyukDefaultPortOnHostNetwork() {
        Mockito.doReturn("host").when(TestcontainersConfiguration.getInstance()).getRyukNetwork();

        RyukContainer container = new RyukContainer();

        assertThat(container.getNetworkMode()).as("host network is applied").isEqualTo("host");
        assertThat(container.getExposedPorts())
            .as("no ports are exposed on the host network, where Docker does not publish ports")
            .isEmpty();
        assertThat(container.getEnv())
            .as("Ryuk listens on its own default port unless one is configured")
            .noneMatch(it -> it.startsWith("RYUK_PORT="));
        assertThat(container.getPort())
            .as("Ryuk's default port is used directly on the Docker host, instead of a JVM-picked port")
            .isEqualTo(8080);
    }

    @Test
    void shouldUseConfiguredPortOnHostNetwork() {
        Mockito.doReturn("host").when(TestcontainersConfiguration.getInstance()).getRyukNetwork();
        Mockito.doReturn(9090).when(TestcontainersConfiguration.getInstance()).getRyukPort();

        RyukContainer container = new RyukContainer();

        assertThat(container.getExposedPorts())
            .as("no ports are exposed on the host network, where Docker does not publish ports")
            .isEmpty();
        assertThat(container.getEnv()).as("the configured port is passed to Ryuk").contains("RYUK_PORT=9090");
        assertThat(container.getPort()).as("the configured port is used directly on the Docker host").isEqualTo(9090);
    }

    @Test
    void shouldUseConfiguredPortOnOtherNetworks() {
        Mockito.doReturn(9090).when(TestcontainersConfiguration.getInstance()).getRyukPort();

        RyukContainer container = new RyukContainer();

        assertThat(container.getExposedPorts())
            .as("the configured port is exposed, so that the daemon publishes it")
            .containsExactly(9090);
        assertThat(container.getEnv()).as("Ryuk is told to listen on the configured port").contains("RYUK_PORT=9090");
    }

    @Test
    void shouldRejectInvalidConfiguredPort() {
        Mockito.doReturn(0).when(TestcontainersConfiguration.getInstance()).getRyukPort();

        assertThatThrownBy(RyukContainer::new)
            .as("a port of 0 would make Ryuk bind an ephemeral port the JVM cannot discover")
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ryuk.container.port");
    }

    @Test
    void shouldUseMappedPortOnOtherNetworks() {
        RyukContainer container = Mockito.spy(new RyukContainer());
        Mockito.doReturn(32768).when(container).getFirstMappedPort();
        container.withNetworkMode("custom-network");

        assertThat(container.getPort())
            .as("the mapped port is used on networks other than the host network")
            .isEqualTo(32768);
    }
}

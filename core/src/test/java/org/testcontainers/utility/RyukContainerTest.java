package org.testcontainers.utility;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldPickFreeHostPortOnHostNetwork() {
        Mockito.doReturn("host").when(TestcontainersConfiguration.getInstance()).getRyukNetwork();

        RyukContainer container = new RyukContainer();

        assertThat(container.getNetworkMode()).as("host network is applied").isEqualTo("host");
        assertThat(container.getExposedPorts())
            .as("no ports are exposed on the host network, where Docker does not publish ports")
            .isEmpty();
        assertThat(container.getEnv())
            .as("the chosen port is passed to Ryuk")
            .contains("RYUK_PORT=" + container.getPort());
        assertThat(container.getPort())
            .as("a host port is chosen directly, instead of waiting for a mapped port")
            .isPositive();
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

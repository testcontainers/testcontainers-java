package org.testcontainers.utility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldUseExposedPortOnHostNetwork() {
        RyukContainer container = new RyukContainer();
        container.withNetworkMode("host");

        assertThat(container.getPort())
            .as("the exposed port is used directly on the host network, where Docker does not publish ports")
            .isEqualTo(8080);
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

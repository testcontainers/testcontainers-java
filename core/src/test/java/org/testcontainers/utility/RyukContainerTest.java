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
        Mockito
            .doReturn("custom-network")
            .when(TestcontainersConfiguration.getInstance())
            .getRyukNetwork();

        assertThat(new RyukContainer().getNetworkMode())
            .as("configured network is applied before the container is started")
            .isEqualTo("custom-network");
    }
}

package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class TestcontainersRestartBetweenTests {

    @Container
    private GenericContainer<?> genericContainer = new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE)
        .withExposedPorts(80);

    private static String lastContainerId;

    @Test
    void first_test() {
        if (lastContainerId == null) {
            lastContainerId = genericContainer.getContainerId();
        } else {
            assertThat(genericContainer.getContainerId()).isNotEqualTo(lastContainerId);
        }
    }

    @Test
    void second_test() {
        if (lastContainerId == null) {
            lastContainerId = genericContainer.getContainerId();
        } else {
            assertThat(genericContainer.getContainerId()).isNotEqualTo(lastContainerId);
        }
    }
}

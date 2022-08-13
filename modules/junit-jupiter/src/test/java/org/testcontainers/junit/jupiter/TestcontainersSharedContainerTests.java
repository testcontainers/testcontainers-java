package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class TestcontainersSharedContainerTests {

    @Container
    private static final GenericContainer<?> GENERIC_CONTAINER = new GenericContainer<>(
        JUnitJupiterTestImages.HTTPD_IMAGE
    )
        .withExposedPorts(80);

    private static String lastContainerId;

    @BeforeAll
    static void doSomethingWithAContainer() {
        assertThat(GENERIC_CONTAINER.isRunning()).isTrue();
    }

    @Test
    void first_test() {
        if (lastContainerId == null) {
            lastContainerId = GENERIC_CONTAINER.getContainerId();
        } else {
            assertThat(GENERIC_CONTAINER.getContainerId()).isEqualTo(lastContainerId);
        }
    }

    @Test
    void second_test() {
        if (lastContainerId == null) {
            lastContainerId = GENERIC_CONTAINER.getContainerId();
        } else {
            assertThat(GENERIC_CONTAINER.getContainerId()).isEqualTo(lastContainerId);
        }
    }
}

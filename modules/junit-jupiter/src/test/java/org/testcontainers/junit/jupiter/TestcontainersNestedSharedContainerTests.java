package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class TestcontainersNestedSharedContainerTests {

    @Container
    private static final GenericContainer<?> TOP_LEVEL_CONTAINER = new GenericContainer<>(
        JUnitJupiterTestImages.HTTPD_IMAGE
    )
        .withExposedPorts(80);

    private static String topLevelContainerId;

    @Test
    void top_level_container_should_be_running() {
        assertThat(TOP_LEVEL_CONTAINER.isRunning()).isTrue();
        topLevelContainerId = TOP_LEVEL_CONTAINER.getContainerId();
    }

    @Nested
    class NestedTestCase {

        @Test
        void top_level_containers_should_be_running() {
            assertThat(TOP_LEVEL_CONTAINER.isRunning()).isTrue();
        }

        @Test
        void ids_should_not_change() {
            assertThat(TOP_LEVEL_CONTAINER.getContainerId()).isEqualTo(topLevelContainerId);
        }
    }
}

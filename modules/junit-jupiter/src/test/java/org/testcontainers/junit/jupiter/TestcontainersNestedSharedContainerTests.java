package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.junit.jupiter.JUnitJupiterTestImages.HTTPD_IMAGE;

@Testcontainers
class TestcontainersNestedSharedContainerTests {

    @Container
    private static final GenericContainer<?> TOP_LEVEL_CONTAINER = new GenericContainer<>(HTTPD_IMAGE)
        .withExposedPorts(80);

    private static String topLevelContainerId;

    @Test
    void top_level_container_should_be_running() {
        assertTrue(TOP_LEVEL_CONTAINER.isRunning());
        topLevelContainerId = TOP_LEVEL_CONTAINER.getContainerId();
    }

    @Nested
    class NestedTestCase {

        @Test
        void top_level_containers_should_be_running() {
            assertTrue(TOP_LEVEL_CONTAINER.isRunning());
        }

        @Test
        void ids_should_not_change() {
            assertEquals(topLevelContainerId, TOP_LEVEL_CONTAINER.getContainerId());
        }
    }
}

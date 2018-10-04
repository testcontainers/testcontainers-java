package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class TestcontainersNestedRestaredContainerIT {

    @Restarted
    private final GenericContainer topLevelContainer = new GenericContainer("httpd:2.4-alpine")
        .withExposedPorts(80);

    private static String TOP_LEVEL_CONTAINER_ID;

    private static String NESTED_CONTAINER_ID;

    @Test
    void top_level_container_should_be_running() {
        assertTrue(topLevelContainer.isRunning());
        TOP_LEVEL_CONTAINER_ID = topLevelContainer.getContainerId();
    }

    @Nested
    class NestedTestCase {

        @Restarted
        private final GenericContainer nestedContainer = new GenericContainer("httpd:2.4-alpine")
            .withExposedPorts(80);

        @Test
        void both_containers_should_be_running() {
            assertTrue(topLevelContainer.isRunning());
            assertTrue(nestedContainer.isRunning());

            if (NESTED_CONTAINER_ID == null) {
                NESTED_CONTAINER_ID = nestedContainer.getContainerId();
            } else {
                assertNotEquals(NESTED_CONTAINER_ID, nestedContainer.getContainerId());
            }
        }

        @Test
        void containers_should_not_be_the_same() {
            assertNotEquals(topLevelContainer.getContainerId(), nestedContainer.getContainerId());

            if (NESTED_CONTAINER_ID == null) {
                NESTED_CONTAINER_ID = nestedContainer.getContainerId();
            } else {
                assertNotEquals(NESTED_CONTAINER_ID, nestedContainer.getContainerId());
            }
        }

        @Test
        void ids_should_not_change() {
            assertNotEquals(TOP_LEVEL_CONTAINER_ID, topLevelContainer.getContainerId());

            if (NESTED_CONTAINER_ID == null) {
                NESTED_CONTAINER_ID = nestedContainer.getContainerId();
            } else {
                assertNotEquals(NESTED_CONTAINER_ID, nestedContainer.getContainerId());
            }
        }
    }
}

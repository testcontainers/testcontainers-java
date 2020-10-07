package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.junit.jupiter.JUnitJupiterTestImages.HTTPD_IMAGE;

@Testcontainers
class TestcontainersSharedContainerTests {

    @Container
    private static final GenericContainer<?> GENERIC_CONTAINER = new GenericContainer<>(HTTPD_IMAGE)
        .withExposedPorts(80);

    private static String lastContainerId;

    @BeforeAll
    static void doSomethingWithAContainer() {
        assertTrue(GENERIC_CONTAINER.isRunning());
    }

    @Test
    void first_test() {
        if (lastContainerId == null) {
            lastContainerId = GENERIC_CONTAINER.getContainerId();
        } else {
            assertEquals(lastContainerId, GENERIC_CONTAINER.getContainerId());
        }
    }

    @Test
    void second_test() {
        if (lastContainerId == null) {
            lastContainerId = GENERIC_CONTAINER.getContainerId();
        } else {
            assertEquals(lastContainerId, GENERIC_CONTAINER.getContainerId());
        }
    }

}

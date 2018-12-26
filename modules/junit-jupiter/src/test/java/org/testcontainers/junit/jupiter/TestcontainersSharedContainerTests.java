package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class TestcontainersSharedContainerTests {

    @Container
    private static final GenericContainer GENERIC_CONTAINER = new GenericContainer("httpd:2.4-alpine")
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

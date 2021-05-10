package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.testcontainers.junit.jupiter.JUnitJupiterTestImages.HTTPD_IMAGE;

@Testcontainers
class TestcontainersRestartBetweenTests {

    @Container
    private GenericContainer<?> genericContainer = new GenericContainer<>(HTTPD_IMAGE)
            .withExposedPorts(80);

    private static String lastContainerId;

    @Test
    void first_test() {
        if (lastContainerId == null) {
            lastContainerId = genericContainer.getContainerId();
        }  else {
            assertNotEquals(lastContainerId, genericContainer.getContainerId());
        }
    }

    @Test
    void second_test() {
        if (lastContainerId == null) {
            lastContainerId = genericContainer.getContainerId();
        }  else {
            assertNotEquals(lastContainerId, genericContainer.getContainerId());
        }
    }

}

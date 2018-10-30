package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Testcontainers
class TestcontainersRestartBetweenTests {

    @Container
    private GenericContainer genericContainer = new GenericContainer("httpd:2.4-alpine")
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

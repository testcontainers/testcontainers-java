package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Testcontainers
class TestcontainersRestartBetweenTestsIT {

    @Restarted
    private GenericContainer genericContainer = new GenericContainer("httpd:2.4-alpine")
            .withExposedPorts(80);

    private static String LAST_CONTAINER_ID;

    @Test
    void first_test() {
        if (LAST_CONTAINER_ID == null) {
            LAST_CONTAINER_ID = genericContainer.getContainerId();
        }  else {
            assertNotEquals(LAST_CONTAINER_ID, genericContainer.getContainerId());
        }
    }

    @Test
    void second_test() {
        if (LAST_CONTAINER_ID == null) {
            LAST_CONTAINER_ID = genericContainer.getContainerId();
        }  else {
            assertNotEquals(LAST_CONTAINER_ID, genericContainer.getContainerId());
        }
    }

}

package org.testcontainers.junit.jqwik;

import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.BeforeContainer;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.junit.jqwik.JUnitJqwikTestImages.HTTPD_IMAGE;

@Testcontainers
class TestcontainersSharedContainerTests {

    @Container
    private static final GenericContainer<?> GENERIC_CONTAINER = new GenericContainer<>(HTTPD_IMAGE)
        .withExposedPorts(80);

    private static String lastContainerId;

    @BeforeContainer
    static void doSomethingWithAContainer() {
        assertTrue(GENERIC_CONTAINER.isRunning());
    }

    @Property
    void first_test() {
        if (lastContainerId == null) {
            lastContainerId = GENERIC_CONTAINER.getContainerId();
        } else {
            assertEquals(lastContainerId, GENERIC_CONTAINER.getContainerId());
        }
    }

    @Property
    void second_test() {
        if (lastContainerId == null) {
            lastContainerId = GENERIC_CONTAINER.getContainerId();
        } else {
            assertEquals(lastContainerId, GENERIC_CONTAINER.getContainerId());
        }
    }

}

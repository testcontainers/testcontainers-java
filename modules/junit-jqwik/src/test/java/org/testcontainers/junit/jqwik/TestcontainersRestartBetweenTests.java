package org.testcontainers.junit.jqwik;

import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.junit.jqwik.JUnitJqwikTestImages.HTTPD_IMAGE;

@Testcontainers
class TestcontainersRestartBetweenTests {

    @TestContainer
    private GenericContainer<?> genericContainer = new GenericContainer<>(HTTPD_IMAGE)
            .withExposedPorts(80);

    private static String lastContainerId;
    private static String currentProperty;

    @BeforeProperty
    void container_is_running(){
        assertTrue(genericContainer.isRunning());
    }

    @AfterProperty
    void container_is_stopped(){
        assertTrue(genericContainer.isRunning());
    }

    @Property
    void first_test() {
        if (lastContainerId == null) {
            currentProperty = "first";
            lastContainerId = genericContainer.getContainerId();
        }  else if(!currentProperty.equals("first")) {
            assertNotEquals(lastContainerId, genericContainer.getContainerId());
        }
    }

    @Property
    void second_test() {
        if (lastContainerId == null) {
            lastContainerId = genericContainer.getContainerId();
            currentProperty = "second";
        }  else if(!currentProperty.equals("second")){
            assertNotEquals(lastContainerId, genericContainer.getContainerId());
        }
    }

}

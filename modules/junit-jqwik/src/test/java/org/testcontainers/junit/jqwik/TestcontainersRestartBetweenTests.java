package org.testcontainers.junit.jqwik;

import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.AfterProperty;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.junit.jqwik.JqwikTestImages.HTTPD_IMAGE;

@Testcontainers
class TestcontainersRestartBetweenTests {

    @Container
    private GenericContainer<?> genericContainer = new GenericContainer<>(HTTPD_IMAGE)
            .withExposedPorts(80);

    private static String lastContainerId;
    private static String currentProperty;

    @BeforeProperty
    void container_is_running(){
        assertThat(genericContainer.isRunning()).isTrue();;
    }

    @AfterProperty
    void container_is_stopped(){
        assertThat(genericContainer.isRunning()).isTrue();;
    }

    @Property
    void first_test() {
        if (lastContainerId == null) {
            currentProperty = "first";
            lastContainerId = genericContainer.getContainerId();
        }  else if(!currentProperty.equals("first")) {
            assertThat(lastContainerId).isNotEqualTo(genericContainer.getContainerId());
        }
    }

    @Property
    void second_test() {
        if (lastContainerId == null) {
            lastContainerId = genericContainer.getContainerId();
            currentProperty = "second";
        }  else if(!currentProperty.equals("second")){
            assertThat(lastContainerId).isNotEqualTo(genericContainer.getContainerId());
        }
    }

}

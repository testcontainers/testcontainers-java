package org.testcontainers.junit.jqwik;

import net.jqwik.api.Example;
import net.jqwik.api.Group;
import net.jqwik.api.lifecycle.BeforeContainer;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.junit.jqwik.JqwikTestImages.HTTPD_IMAGE;

@Testcontainers
class TestcontainersGroupedSharedContainerTests {

    @Container
    private static final GenericContainer<?> TOP_LEVEL_CONTAINER = new GenericContainer<>(HTTPD_IMAGE)
        .withExposedPorts(80);

    private static String topLevelContainerId;

    @BeforeContainer
    static void setTopLevelContainer(){
        topLevelContainerId = TOP_LEVEL_CONTAINER.getContainerId();
    }

    @Example
    void top_level_container_should_be_running() {
        assertThat(TOP_LEVEL_CONTAINER.isRunning()).isTrue();
    }

    @Group
    class GroupedExamples {

        @Example
        void top_level_containers_should_be_running() {
            assertThat(TOP_LEVEL_CONTAINER.isRunning()).isTrue();
        }

        @Example
        void ids_should_not_change() {
            assertThat(topLevelContainerId).isEqualTo(TOP_LEVEL_CONTAINER.getContainerId());
        }
    }
}

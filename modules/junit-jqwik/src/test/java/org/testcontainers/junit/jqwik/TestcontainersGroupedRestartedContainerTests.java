package org.testcontainers.junit.jqwik;

import net.jqwik.api.Disabled;
import net.jqwik.api.Example;
import net.jqwik.api.Group;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.junit.jqwik.JqwikTestImages.HTTPD_IMAGE;

@Disabled("A PropertyLifecyclyeContext does not provide the means to access the parent instance." +
    "This makes it hard to access the parent test instance. The nested container is started and stopped. " +
    "Different to Jupiter, Jqwik traverses the tree of test elements bottom-up and first runs the properties" +
    "within a group.")
@Testcontainers
class TestcontainersGroupedRestartedContainerTests {

    @Container
    private final GenericContainer<?> topLevelContainer = new GenericContainer<>(HTTPD_IMAGE)
        .withExposedPorts(80);

    private static String topLevelContainerId;

    private static String groupedContainerId;

    @BeforeProperty
    void setTopLevelContainerId(){
        topLevelContainerId = topLevelContainer.getContainerId();
    }

    @Example
    void top_level_container_should_be_running() {
        assertThat(topLevelContainer.isRunning()).isTrue();
    }

    @Group
    class NestedTestCase {

        @Container
        private final GenericContainer<?> groupedContainer = new GenericContainer<>(HTTPD_IMAGE)
            .withExposedPorts(80);

        @Example
        void both_containers_should_be_running() {
            // top level container is restarted for nested methods
            assertThat(topLevelContainer.isRunning()).isTrue();
            // nested containers are only available inside their nested class
            assertThat(groupedContainer.isRunning()).isTrue();
            if (groupedContainerId == null) {
                groupedContainerId = groupedContainer.getContainerId();
            } else {
                assertThat(groupedContainerId).isNotEqualTo(groupedContainer.getContainerId());
            }
        }

        @Example
        void containers_should_not_be_the_same() {
            assertThat(topLevelContainer.getContainerId()).isNotEqualTo(groupedContainer.getContainerId());

            if (groupedContainerId == null) {
                groupedContainerId = groupedContainer.getContainerId();
            } else {
                assertThat(groupedContainerId).isNotEqualTo(groupedContainer.getContainerId());
            }
        }

        @Example
        void ids_should_not_change() {
            assertThat(topLevelContainerId).isNotEqualTo(topLevelContainer.getContainerId());

            if (groupedContainerId == null) {
                groupedContainerId = groupedContainer.getContainerId();
            } else {
                assertThat(groupedContainerId).isNotEqualTo(groupedContainer.getContainerId());
            }
        }
    }
}

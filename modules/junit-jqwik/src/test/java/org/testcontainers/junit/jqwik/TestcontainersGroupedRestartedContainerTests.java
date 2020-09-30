package org.testcontainers.junit.jqwik;

import net.jqwik.api.Example;
import net.jqwik.api.Group;
import net.jqwik.api.lifecycle.BeforeProperty;
import org.junit.jupiter.api.Disabled;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.junit.jqwik.JUnitJqwikTestImages.HTTPD_IMAGE;

@Disabled("A PropertyLifecyclyeContext does not provide the means to access the parent instance." +
    "This makes it hard to access the parent test instance. The nested container is started and stopped. " +
    "Different to Jupiter, Jqwik traverses the tree of test elements bottom-up and first runs the properties" +
    "within a group.")
// testClass {
@Testcontainers
class TestcontainersGroupedRestartedContainerTests {

    @TestContainer
    private final GenericContainer<?> topLevelContainer = new GenericContainer<>(HTTPD_IMAGE)
        .withExposedPorts(80);
    // }}

    private static String topLevelContainerId;

    private static String groupedContainerId;

    @BeforeProperty
    void setTopLevelContainerId(){
        topLevelContainerId = topLevelContainer.getContainerId();
    }

    // testClass {
    @Example
    void top_level_container_should_be_running() {
        assertTrue(topLevelContainer.isRunning());
// }}
// testClass {{
    }

    @Group
    class NestedTestCase {

        @TestContainer
        private final GenericContainer<?> groupedContainer = new GenericContainer<>(HTTPD_IMAGE)
            .withExposedPorts(80);

        @Example
        void both_containers_should_be_running() {
            // top level container is restarted for nested methods
            assertTrue(topLevelContainer.isRunning());
            // nested containers are only available inside their nested class
            assertTrue(groupedContainer.isRunning());
// }}}
            if (groupedContainerId == null) {
                groupedContainerId = groupedContainer.getContainerId();
            } else {
                assertNotEquals(groupedContainerId, groupedContainer.getContainerId());
            }
// testClass {{
        }

        // }
        @Example
        void containers_should_not_be_the_same() {
            assertNotEquals(topLevelContainer.getContainerId(), groupedContainer.getContainerId());

            if (groupedContainerId == null) {
                groupedContainerId = groupedContainer.getContainerId();
            } else {
                assertNotEquals(groupedContainerId, groupedContainer.getContainerId());
            }
        }

        @Example
        void ids_should_not_change() {
            assertNotEquals(topLevelContainerId, topLevelContainer.getContainerId());

            if (groupedContainerId == null) {
                groupedContainerId = groupedContainer.getContainerId();
            } else {
                assertNotEquals(groupedContainerId, groupedContainer.getContainerId());
            }
        }
// testClass {{{
    }
}
// }

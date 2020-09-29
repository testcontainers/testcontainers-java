package org.testcontainers.junit.jqwik;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.junit.jqwik.JUnitJqwikTestImages.HTTPD_IMAGE;

// testClass {
@Testcontainers
class TestcontainersNestedRestartedContainerTests {

    @TestContainer
    private final GenericContainer<?> topLevelContainer = new GenericContainer<>(HTTPD_IMAGE)
        .withExposedPorts(80);
    // }}

    private static String topLevelContainerId;

    private static String nestedContainerId;

    // testClass {
    @Test
    void top_level_container_should_be_running() {
        assertTrue(topLevelContainer.isRunning());
// }}
        topLevelContainerId = topLevelContainer.getContainerId();
// testClass {{
    }

    @Nested
    class NestedTestCase {

        @TestContainer
        private final GenericContainer<?> nestedContainer = new GenericContainer<>(HTTPD_IMAGE)
            .withExposedPorts(80);

        @Test
        void both_containers_should_be_running() {
            // top level container is restarted for nested methods
            assertTrue(topLevelContainer.isRunning());
            // nested containers are only available inside their nested class
            assertTrue(nestedContainer.isRunning());
// }}}
            if (nestedContainerId == null) {
                nestedContainerId = nestedContainer.getContainerId();
            } else {
                assertNotEquals(nestedContainerId, nestedContainer.getContainerId());
            }
// testClass {{
        }

        // }
        @Test
        void containers_should_not_be_the_same() {
            assertNotEquals(topLevelContainer.getContainerId(), nestedContainer.getContainerId());

            if (nestedContainerId == null) {
                nestedContainerId = nestedContainer.getContainerId();
            } else {
                assertNotEquals(nestedContainerId, nestedContainer.getContainerId());
            }
        }

        @Test
        void ids_should_not_change() {
            assertNotEquals(topLevelContainerId, topLevelContainer.getContainerId());

            if (nestedContainerId == null) {
                nestedContainerId = nestedContainer.getContainerId();
            } else {
                assertNotEquals(nestedContainerId, nestedContainer.getContainerId());
            }
        }
// testClass {{{
    }
}
// }

package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

// testClass {
@Testcontainers
class TestcontainersNestedRestartedContainerTests {

    @Container
    private final GenericContainer<?> topLevelContainer = new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE)
        .withExposedPorts(80);

    // }}

    private static String topLevelContainerId;

    private static String nestedContainerId;

    // testClass {
    @Test
    void top_level_container_should_be_running() {
        assertThat(topLevelContainer.isRunning()).isTrue();
        // }}
        topLevelContainerId = topLevelContainer.getContainerId();
        // testClass {{
    }

    @Nested
    class NestedTestCase {

        @Container
        private final GenericContainer<?> nestedContainer = new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE)
            .withExposedPorts(80);

        @Test
        void both_containers_should_be_running() {
            // top level container is restarted for nested methods
            assertThat(topLevelContainer.isRunning()).isTrue();
            // nested containers are only available inside their nested class
            assertThat(nestedContainer.isRunning()).isTrue();
            // }}}
            if (nestedContainerId == null) {
                nestedContainerId = nestedContainer.getContainerId();
            } else {
                assertThat(nestedContainer.getContainerId()).isNotEqualTo(nestedContainerId);
            }
            // testClass {{
        }

        // }
        @Test
        void containers_should_not_be_the_same() {
            assertThat(nestedContainer.getContainerId()).isNotEqualTo(topLevelContainer.getContainerId());

            if (nestedContainerId == null) {
                nestedContainerId = nestedContainer.getContainerId();
            } else {
                assertThat(nestedContainer.getContainerId()).isNotEqualTo(nestedContainerId);
            }
        }

        @Test
        void ids_should_not_change() {
            assertThat(topLevelContainer.getContainerId()).isNotEqualTo(topLevelContainerId);

            if (nestedContainerId == null) {
                nestedContainerId = nestedContainer.getContainerId();
            } else {
                assertThat(nestedContainer.getContainerId()).isNotEqualTo(nestedContainerId);
            }
        }
        // testClass {{{
    }
}
// }

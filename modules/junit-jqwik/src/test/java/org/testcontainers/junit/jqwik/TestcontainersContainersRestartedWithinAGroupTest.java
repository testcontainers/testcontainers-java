package org.testcontainers.junit.jqwik;

import net.jqwik.api.Example;
import net.jqwik.api.Group;
import org.testcontainers.containers.GenericContainer;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.testcontainers.junit.jqwik.JUnitJqwikTestImages.HTTPD_IMAGE;

@Testcontainers
public class TestcontainersContainersRestartedWithinAGroupTest {

    @Container
    private final GenericContainer<?> topLevelContainer = new GenericContainer<>(HTTPD_IMAGE)
        .withExposedPorts(80);

    @Example
    public void top_level_example(){
        assertTrue(topLevelContainer.isRunning());
    }

    @Group
    public class Group1 {

        @Container
        private final GenericContainer<?> groupedContainer = new GenericContainer<>(HTTPD_IMAGE)
            .withExposedPorts(80);

        private String lastGroupedContainerId;

        @Example
        public void example_with_grouped_container(){
            assertTrue(groupedContainer.isRunning());
            if (lastGroupedContainerId == null) {
                lastGroupedContainerId = groupedContainer.getContainerId();
            } else {
                assertNotEquals(lastGroupedContainerId, groupedContainer.getContainerId());
            }
        }

        @Example
        public void other_example_with_grouped_container(){
            assertTrue(groupedContainer.isRunning());
            if (lastGroupedContainerId == null) {
                lastGroupedContainerId = groupedContainer.getContainerId();
            } else {
                assertNotEquals(lastGroupedContainerId, groupedContainer.getContainerId());
            }
        }
    }

    @Group
    public class Group2 {

        @Container
        private final GenericContainer<?> groupedContainer = new GenericContainer<>(HTTPD_IMAGE)
            .withExposedPorts(80);

        private String lastGroupedContainerId;

        @Example
        public void example_with_grouped_container(){
            assertTrue(groupedContainer.isRunning());
            if (lastGroupedContainerId == null) {
                lastGroupedContainerId = groupedContainer.getContainerId();
            } else {
                assertNotEquals(lastGroupedContainerId, groupedContainer.getContainerId());
            }
        }

        @Example
        public void other_example_with_grouped_container(){
            assertTrue(groupedContainer.isRunning());
            if (lastGroupedContainerId == null) {
                lastGroupedContainerId = groupedContainer.getContainerId();
            } else {
                assertNotEquals(lastGroupedContainerId, groupedContainer.getContainerId());
            }
        }
    }
}

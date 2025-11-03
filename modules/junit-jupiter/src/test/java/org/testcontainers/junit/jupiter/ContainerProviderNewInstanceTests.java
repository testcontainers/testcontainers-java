package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the needNewInstance feature of @ContainerConfig.
 */
@Testcontainers
class ContainerProviderNewInstanceTests {

    private static final Set<String> containerIds = new HashSet<>();

    @ContainerProvider(name = "testContainer", scope = ContainerProvider.Scope.CLASS)
    public GenericContainer<?> createContainer() {
        return new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE).withExposedPorts(80);
    }

    @Test
    @ContainerConfig(name = "testContainer", needNewInstance = false, injectAsParameter = true)
    void testReusedContainer1(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        containerIds.add(container.getContainerId());
    }

    @Test
    @ContainerConfig(name = "testContainer", needNewInstance = false, injectAsParameter = true)
    void testReusedContainer2(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        containerIds.add(container.getContainerId());
        
        // After two tests with needNewInstance=false, we should have only 1 unique container ID
        assertThat(containerIds).hasSize(1);
    }

    @Test
    @ContainerConfig(name = "testContainer", needNewInstance = true, injectAsParameter = true)
    void testNewInstance1(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        
        String newContainerId = container.getContainerId();
        assertThat(newContainerId).isNotNull();
        
        // This should be a different container
        containerIds.add(newContainerId);
        assertThat(containerIds).hasSizeGreaterThan(1);
    }

    @Test
    @ContainerConfig(name = "testContainer", needNewInstance = true, injectAsParameter = true)
    void testNewInstance2(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        
        String newContainerId = container.getContainerId();
        assertThat(newContainerId).isNotNull();
        
        // This should be yet another different container
        containerIds.add(newContainerId);
        assertThat(containerIds).hasSizeGreaterThan(2);
    }
}

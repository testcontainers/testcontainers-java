package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests parameter injection with @ContainerConfig.
 */
@Testcontainers
class ContainerProviderParameterInjectionTests {

    @ContainerProvider(name = "httpd", scope = ContainerProvider.Scope.CLASS)
    public GenericContainer<?> createHttpd() {
        return new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE).withExposedPorts(80);
    }

    @Test
    @ContainerConfig(name = "httpd", injectAsParameter = true)
    void testParameterInjection(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        assertThat(container.getExposedPorts()).contains(80);
    }

    @Test
    @ContainerConfig(name = "httpd", injectAsParameter = true)
    void testParameterInjectionSecondTest(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        
        // Verify we can get connection details
        String host = container.getHost();
        Integer port = container.getFirstMappedPort();
        
        assertThat(host).isNotNull();
        assertThat(port).isGreaterThan(0);
    }

    @Test
    @ContainerConfig(name = "httpd", injectAsParameter = true)
    void testContainerIdConsistency(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        String containerId = container.getContainerId();
        assertThat(containerId).isNotNull();
        assertThat(containerId).isNotEmpty();
    }
}

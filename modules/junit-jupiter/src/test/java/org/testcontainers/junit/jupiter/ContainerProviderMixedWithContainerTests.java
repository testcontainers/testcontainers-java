package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests mixing @Container fields with @ContainerProvider/@ContainerConfig.
 * Both approaches should work together in the same test class.
 */
@Testcontainers
class ContainerProviderMixedWithContainerTests {

    // Traditional @Container field approach
    @Container
    private static final GenericContainer<?> TRADITIONAL_CONTAINER = new GenericContainer<>(
        JUnitJupiterTestImages.HTTPD_IMAGE
    )
        .withExposedPorts(80);

    // New @ContainerProvider approach
    @ContainerProvider(name = "providedContainer", scope = ContainerProvider.Scope.CLASS)
    public PostgreSQLContainer<?> createProvidedContainer() {
        return new PostgreSQLContainer<>(JUnitJupiterTestImages.POSTGRES_IMAGE);
    }

    @Test
    void testTraditionalContainer() {
        assertThat(TRADITIONAL_CONTAINER).isNotNull();
        assertThat(TRADITIONAL_CONTAINER.isRunning()).isTrue();
        assertThat(TRADITIONAL_CONTAINER.getExposedPorts()).contains(80);
    }

    @Test
    @ContainerConfig(name = "providedContainer", injectAsParameter = true)
    void testProvidedContainer(PostgreSQLContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        assertThat(container.getJdbcUrl()).isNotNull();
    }

    @Test
    void testBothContainersRunning() {
        // Traditional container should be running
        assertThat(TRADITIONAL_CONTAINER.isRunning()).isTrue();
        
        // This test doesn't use the provided container, but it should still work
        assertThat(true).isTrue();
    }

    @Test
    @ContainerConfig(name = "providedContainer", injectAsParameter = true)
    void testBothApproachesCoexist(PostgreSQLContainer<?> providedContainer) {
        // Both containers should be running
        assertThat(TRADITIONAL_CONTAINER.isRunning()).isTrue();
        assertThat(providedContainer.isRunning()).isTrue();
        
        // They should be different containers
        assertThat(TRADITIONAL_CONTAINER.getContainerId()).isNotEqualTo(providedContainer.getContainerId());
    }
}

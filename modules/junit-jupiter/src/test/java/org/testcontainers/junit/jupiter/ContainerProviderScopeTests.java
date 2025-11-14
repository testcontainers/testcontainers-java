package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests different container scopes (CLASS vs GLOBAL).
 */
@Testcontainers
class ContainerProviderScopeTests {

    @ContainerProvider(name = "classScoped", scope = ContainerProvider.Scope.CLASS)
    public GenericContainer<?> createClassScoped() {
        return new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE).withExposedPorts(80);
    }

    @ContainerProvider(name = "globalScoped", scope = ContainerProvider.Scope.GLOBAL)
    public GenericContainer<?> createGlobalScoped() {
        return new GenericContainer<>(JUnitJupiterTestImages.MYSQL_IMAGE).withExposedPorts(3306);
    }

    @Test
    @ContainerConfig(name = "classScoped", injectAsParameter = true)
    void testClassScopedContainer1(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        assertThat(container.getExposedPorts()).contains(80);
    }

    @Test
    @ContainerConfig(name = "classScoped", injectAsParameter = true)
    void testClassScopedContainer2(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        // Should be the same instance as test1
    }

    @Test
    @ContainerConfig(name = "globalScoped", injectAsParameter = true)
    void testGlobalScopedContainer1(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        assertThat(container.getExposedPorts()).contains(3306);
    }

    @Test
    @ContainerConfig(name = "globalScoped", injectAsParameter = true)
    void testGlobalScopedContainer2(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        // Should be the same instance across all test classes
    }
}

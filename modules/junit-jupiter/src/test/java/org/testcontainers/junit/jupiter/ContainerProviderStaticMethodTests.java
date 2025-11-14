package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests static provider methods.
 */
@Testcontainers
class ContainerProviderStaticMethodTests {

    @ContainerProvider(name = "staticHttpd", scope = ContainerProvider.Scope.CLASS)
    public static GenericContainer<?> createStaticHttpd() {
        return new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE).withExposedPorts(80);
    }

    @ContainerProvider(name = "instancePostgres", scope = ContainerProvider.Scope.CLASS)
    public PostgreSQLContainer<?> createInstancePostgres() {
        return new PostgreSQLContainer<>(JUnitJupiterTestImages.POSTGRES_IMAGE);
    }

    @Test
    @ContainerConfig(name = "staticHttpd", injectAsParameter = true)
    void testStaticProvider(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        assertThat(container.getExposedPorts()).contains(80);
    }

    @Test
    @ContainerConfig(name = "instancePostgres", injectAsParameter = true)
    void testInstanceProvider(PostgreSQLContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        assertThat(container.getJdbcUrl()).contains("jdbc:postgresql://");
    }

    @Test
    @ContainerConfig(name = "staticHttpd", injectAsParameter = true)
    void testStaticProviderReuse(GenericContainer<?> container) {
        // Should reuse the same container
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
    }

    @Test
    @ContainerConfig(name = "instancePostgres", injectAsParameter = true)
    void testInstanceProviderReuse(PostgreSQLContainer<?> container) {
        // Should reuse the same container
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
    }
}

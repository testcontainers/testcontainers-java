package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests multiple container providers in the same test class.
 */
@Testcontainers
class ContainerProviderMultipleProvidersTests {

    @ContainerProvider(name = "httpd", scope = ContainerProvider.Scope.CLASS)
    public GenericContainer<?> createHttpd() {
        return new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE).withExposedPorts(80);
    }

    @ContainerProvider(name = "postgres", scope = ContainerProvider.Scope.CLASS)
    public PostgreSQLContainer<?> createPostgres() {
        return new PostgreSQLContainer<>(JUnitJupiterTestImages.POSTGRES_IMAGE);
    }

    @ContainerProvider(name = "mysql", scope = ContainerProvider.Scope.CLASS)
    public GenericContainer<?> createMysql() {
        return new GenericContainer<>(JUnitJupiterTestImages.MYSQL_IMAGE).withExposedPorts(3306);
    }

    @Test
    @ContainerConfig(name = "httpd", injectAsParameter = true)
    void testHttpdContainer(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        assertThat(container.getExposedPorts()).contains(80);
    }

    @Test
    @ContainerConfig(name = "postgres", injectAsParameter = true)
    void testPostgresContainer(PostgreSQLContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        assertThat(container.getJdbcUrl()).isNotNull();
        assertThat(container.getUsername()).isNotNull();
    }

    @Test
    @ContainerConfig(name = "mysql", injectAsParameter = true)
    void testMysqlContainer(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        assertThat(container.getExposedPorts()).contains(3306);
    }

    @Test
    @ContainerConfig(name = "httpd", injectAsParameter = true)
    void testHttpdContainerAgain(GenericContainer<?> container) {
        // Should reuse the same httpd container
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
    }
}

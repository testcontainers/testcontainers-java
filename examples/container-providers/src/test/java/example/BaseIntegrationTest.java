package example;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.ContainerProvider;

/**
 * Base class defining shared container providers for integration tests.
 * 
 * This demonstrates the solution to the feature request:
 * "Containers that are needed for multiple tests in multiple classes 
 * only need to be defined once and the instances can be reused."
 */
public abstract class BaseIntegrationTest {

    /**
     * Shared PostgreSQL database container.
     * This container will be started once and reused across all test classes.
     * 
     * Benefits:
     * - Faster test execution (no repeated container startup)
     * - Consistent test environment
     * - Reduced resource usage
     */
    @ContainerProvider(name = "database", scope = ContainerProvider.Scope.GLOBAL)
    public PostgreSQLContainer<?> createDatabase() {
        return new PostgreSQLContainer<>("postgres:14-alpine")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withInitScript("init-schema.sql");
    }

    /**
     * Shared Redis cache container.
     * Also started once and reused across all test classes.
     */
    @ContainerProvider(name = "cache", scope = ContainerProvider.Scope.GLOBAL)
    public GenericContainer<?> createCache() {
        return new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);
    }

    /**
     * Message queue container with class scope.
     * A new instance is created for each test class to ensure isolation.
     */
    @ContainerProvider(name = "messageQueue", scope = ContainerProvider.Scope.CLASS)
    public GenericContainer<?> createMessageQueue() {
        return new GenericContainer<>("rabbitmq:3-management-alpine")
            .withExposedPorts(5672, 15672);
    }
}

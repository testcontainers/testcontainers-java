package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-world example demonstrating the use case from the feature request:
 * Multiple integration tests across classes reusing the same container instances.
 */
@Testcontainers
class ContainerProviderRealWorldExampleTests {

    /**
     * Shared database container for integration tests.
     * This container will be started once and reused across all tests.
     */
    @ContainerProvider(name = "database", scope = ContainerProvider.Scope.GLOBAL)
    public PostgreSQLContainer<?> createDatabase() {
        return new PostgreSQLContainer<>(JUnitJupiterTestImages.POSTGRES_IMAGE)
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");
    }

    /**
     * Shared cache container (simulated with httpd for testing).
     * This container will be started once and reused across all tests.
     */
    @ContainerProvider(name = "cache", scope = ContainerProvider.Scope.GLOBAL)
    public GenericContainer<?> createCache() {
        return new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE).withExposedPorts(80);
    }

    /**
     * Message queue container that needs fresh instance for each test.
     */
    @ContainerProvider(name = "messageQueue", scope = ContainerProvider.Scope.CLASS)
    public GenericContainer<?> createMessageQueue() {
        return new GenericContainer<>(JUnitJupiterTestImages.MYSQL_IMAGE).withExposedPorts(3306);
    }

    @Test
    @ContainerConfig(name = "database", injectAsParameter = true)
    void testDatabaseConnection(PostgreSQLContainer<?> db) {
        assertThat(db).isNotNull();
        assertThat(db.isRunning()).isTrue();
        
        // Verify database configuration
        assertThat(db.getDatabaseName()).isEqualTo("testdb");
        assertThat(db.getUsername()).isEqualTo("testuser");
        assertThat(db.getPassword()).isEqualTo("testpass");
        
        // Get connection details
        String jdbcUrl = db.getJdbcUrl();
        assertThat(jdbcUrl).contains("jdbc:postgresql://");
        assertThat(jdbcUrl).contains("testdb");
    }

    @Test
    @ContainerConfig(name = "cache", injectAsParameter = true)
    void testCacheConnection(GenericContainer<?> cache) {
        assertThat(cache).isNotNull();
        assertThat(cache.isRunning()).isTrue();
        
        // Verify cache is accessible
        String host = cache.getHost();
        Integer port = cache.getFirstMappedPort();
        
        assertThat(host).isNotNull();
        assertThat(port).isGreaterThan(0);
    }

    @Test
    @ContainerConfig(name = "database", injectAsParameter = true)
    void testDatabaseQuery(PostgreSQLContainer<?> db) {
        // Reuses the same database container from previous test
        assertThat(db).isNotNull();
        assertThat(db.isRunning()).isTrue();
        
        // In a real scenario, you would execute SQL queries here
        assertThat(db.getJdbcUrl()).isNotNull();
    }

    @Test
    @ContainerConfig(name = "messageQueue", needNewInstance = true, injectAsParameter = true)
    void testMessageQueueIsolated(GenericContainer<?> mq) {
        // Gets a fresh message queue instance for isolation
        assertThat(mq).isNotNull();
        assertThat(mq.isRunning()).isTrue();
        
        // This test can modify the message queue without affecting other tests
        assertThat(mq.getExposedPorts()).contains(3306);
    }

    @Test
    @ContainerConfig(name = "messageQueue", needNewInstance = true, injectAsParameter = true)
    void testMessageQueueIsolated2(GenericContainer<?> mq) {
        // Gets another fresh message queue instance
        assertThat(mq).isNotNull();
        assertThat(mq.isRunning()).isTrue();
        
        // This is a different container than the previous test
        assertThat(mq.getContainerId()).isNotNull();
    }

    /**
     * Test using multiple containers simultaneously.
     */
    @Test
    @ContainerConfig(name = "database", injectAsParameter = true)
    void testWithMultipleContainers_Database(PostgreSQLContainer<?> db) {
        assertThat(db).isNotNull();
        assertThat(db.isRunning()).isTrue();
        
        // In a real scenario, this test would:
        // 1. Connect to database
        // 2. Insert test data
        // 3. Verify data persistence
        
        assertThat(db.getJdbcUrl()).isNotNull();
    }

    @Test
    @ContainerConfig(name = "cache", injectAsParameter = true)
    void testWithMultipleContainers_Cache(GenericContainer<?> cache) {
        assertThat(cache).isNotNull();
        assertThat(cache.isRunning()).isTrue();
        
        // In a real scenario, this test would:
        // 1. Connect to cache
        // 2. Store cached values
        // 3. Verify cache hits/misses
        
        assertThat(cache.getHost()).isNotNull();
    }
}

package example;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.ContainerConfig;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for OrderService.
 * 
 * This test class demonstrates:
 * 1. Reusing the SAME database container instance used by UserServiceIntegrationTest
 * 2. Using multiple containers (database + cache) in the same test
 * 3. Performance benefits of container reuse
 */
@Testcontainers
class OrderServiceIntegrationTest extends BaseIntegrationTest {

    @Test
    @ContainerConfig(name = "database", injectAsParameter = true)
    void testCreateOrder(PostgreSQLContainer<?> db) throws Exception {
        // This reuses the SAME database container started by UserServiceIntegrationTest
        // No need to wait for container startup!
        assertThat(db).isNotNull();
        assertThat(db.isRunning()).isTrue();

        try (Connection conn = DriverManager.getConnection(
            db.getJdbcUrl(),
            db.getUsername(),
            db.getPassword()
        )) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS orders (id SERIAL PRIMARY KEY, product VARCHAR(100), quantity INT)");
                stmt.execute("INSERT INTO orders (product, quantity) VALUES ('Laptop', 2)");
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM orders WHERE product = 'Laptop'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("product")).isEqualTo("Laptop");
                assertThat(rs.getInt("quantity")).isEqualTo(2);
            }
        }
    }

    @Test
    @ContainerConfig(name = "database", injectAsParameter = true)
    void testCalculateOrderTotal(PostgreSQLContainer<?> db) throws Exception {
        assertThat(db).isNotNull();
        assertThat(db.isRunning()).isTrue();

        try (Connection conn = DriverManager.getConnection(
            db.getJdbcUrl(),
            db.getUsername(),
            db.getPassword()
        )) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS orders (id SERIAL PRIMARY KEY, product VARCHAR(100), quantity INT, price DECIMAL)");
                stmt.execute("INSERT INTO orders (product, quantity, price) VALUES ('Mouse', 3, 25.50)");
                stmt.execute("INSERT INTO orders (product, quantity, price) VALUES ('Keyboard', 1, 75.00)");
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT SUM(quantity * price) as total FROM orders")) {
                assertThat(rs.next()).isTrue();
                double total = rs.getDouble("total");
                assertThat(total).isGreaterThan(0);
            }
        }
    }

    @Test
    @ContainerConfig(name = "cache", injectAsParameter = true)
    void testOrderCaching(GenericContainer<?> cache) {
        // Uses the shared Redis cache container
        assertThat(cache).isNotNull();
        assertThat(cache.isRunning()).isTrue();
        assertThat(cache.getExposedPorts()).contains(6379);

        // In a real test, you would:
        // 1. Connect to Redis
        // 2. Cache order data
        // 3. Verify cache hits/misses
        String redisHost = cache.getHost();
        Integer redisPort = cache.getFirstMappedPort();

        assertThat(redisHost).isNotNull();
        assertThat(redisPort).isGreaterThan(0);
    }

    @Test
    @ContainerConfig(name = "database", needNewInstance = true, injectAsParameter = true)
    void testWithIsolatedDatabase(PostgreSQLContainer<?> db) throws Exception {
        // This test gets a FRESH database instance for complete isolation
        // Useful for tests that modify schema or require clean state
        assertThat(db).isNotNull();
        assertThat(db.isRunning()).isTrue();

        try (Connection conn = DriverManager.getConnection(
            db.getJdbcUrl(),
            db.getUsername(),
            db.getPassword()
        )) {
            try (Statement stmt = conn.createStatement()) {
                // This database is completely isolated from other tests
                stmt.execute("CREATE TABLE orders (id SERIAL PRIMARY KEY, status VARCHAR(50))");
                stmt.execute("INSERT INTO orders (status) VALUES ('PENDING')");
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM orders")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
        }
    }
}

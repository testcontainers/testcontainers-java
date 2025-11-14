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
 * Integration tests for PaymentService.
 * 
 * This is the THIRD test class using the shared database container.
 * It demonstrates the full benefit of the container provider pattern:
 * - No container startup delay
 * - Consistent test environment
 * - Reduced resource usage
 */
@Testcontainers
class PaymentServiceIntegrationTest extends BaseIntegrationTest {

    @Test
    @ContainerConfig(name = "database", injectAsParameter = true)
    void testProcessPayment(PostgreSQLContainer<?> db) throws Exception {
        // Still using the SAME database container - no startup delay!
        assertThat(db).isNotNull();
        assertThat(db.isRunning()).isTrue();

        try (Connection conn = DriverManager.getConnection(
            db.getJdbcUrl(),
            db.getUsername(),
            db.getPassword()
        )) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS payments (id SERIAL PRIMARY KEY, amount DECIMAL, status VARCHAR(50))");
                stmt.execute("INSERT INTO payments (amount, status) VALUES (99.99, 'COMPLETED')");
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM payments WHERE status = 'COMPLETED'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getDouble("amount")).isEqualTo(99.99);
            }
        }
    }

    @Test
    @ContainerConfig(name = "database", injectAsParameter = true)
    void testRefundPayment(PostgreSQLContainer<?> db) throws Exception {
        assertThat(db).isNotNull();
        assertThat(db.isRunning()).isTrue();

        try (Connection conn = DriverManager.getConnection(
            db.getJdbcUrl(),
            db.getUsername(),
            db.getPassword()
        )) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS payments (id SERIAL PRIMARY KEY, amount DECIMAL, status VARCHAR(50))");
                stmt.execute("INSERT INTO payments (amount, status) VALUES (49.99, 'PENDING')");
                stmt.execute("UPDATE payments SET status = 'REFUNDED' WHERE amount = 49.99");
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT status FROM payments WHERE amount = 49.99")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("status")).isEqualTo("REFUNDED");
            }
        }
    }

    @Test
    @ContainerConfig(name = "messageQueue", injectAsParameter = true)
    void testPaymentNotification(GenericContainer<?> mq) {
        // Uses the message queue container (class-scoped)
        assertThat(mq).isNotNull();
        assertThat(mq.isRunning()).isTrue();
        assertThat(mq.getExposedPorts()).contains(5672);

        // In a real test, you would:
        // 1. Connect to RabbitMQ
        // 2. Send payment notification message
        // 3. Verify message was received
        String mqHost = mq.getHost();
        Integer mqPort = mq.getFirstMappedPort();

        assertThat(mqHost).isNotNull();
        assertThat(mqPort).isGreaterThan(0);
    }

    @Test
    @ContainerConfig(name = "cache", injectAsParameter = true)
    void testPaymentCaching(GenericContainer<?> cache) {
        // Uses the shared Redis cache
        assertThat(cache).isNotNull();
        assertThat(cache.isRunning()).isTrue();

        // In a real test, you would cache payment details
        String cacheHost = cache.getHost();
        Integer cachePort = cache.getFirstMappedPort();

        assertThat(cacheHost).isNotNull();
        assertThat(cachePort).isGreaterThan(0);
    }

    /**
     * Demonstrates using multiple containers in a single test.
     * This simulates a real-world scenario where a payment operation
     * involves database, cache, and message queue.
     */
    @Test
    @ContainerConfig(name = "database", injectAsParameter = true)
    void testCompletePaymentWorkflow_Database(PostgreSQLContainer<?> db) throws Exception {
        // Step 1: Store payment in database
        assertThat(db).isNotNull();
        assertThat(db.isRunning()).isTrue();

        try (Connection conn = DriverManager.getConnection(
            db.getJdbcUrl(),
            db.getUsername(),
            db.getPassword()
        )) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS payments (id SERIAL PRIMARY KEY, amount DECIMAL, status VARCHAR(50))");
                stmt.execute("INSERT INTO payments (amount, status) VALUES (199.99, 'PROCESSING')");
            }

            // Step 2: In a real scenario, you would also:
            // - Cache the payment details (using cache container)
            // - Send notification (using message queue container)
            // - Update payment status

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM payments WHERE status = 'PROCESSING'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isGreaterThan(0);
            }
        }
    }
}

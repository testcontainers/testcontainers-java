package example;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.ContainerConfig;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UserService.
 * 
 * This test class demonstrates:
 * 1. Reusing the shared database container defined in BaseIntegrationTest
 * 2. Parameter injection for type-safe container access
 * 3. No boilerplate code for container lifecycle management
 */
@Testcontainers
class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Test
    @ContainerConfig(name = "database", injectAsParameter = true)
    void testCreateUser(PostgreSQLContainer<?> db) throws Exception {
        // The database container is automatically started and injected
        assertThat(db).isNotNull();
        assertThat(db.isRunning()).isTrue();

        // Connect to the database
        try (Connection conn = DriverManager.getConnection(
            db.getJdbcUrl(),
            db.getUsername(),
            db.getPassword()
        )) {
            // Create a user
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS users (id SERIAL PRIMARY KEY, name VARCHAR(100))");
                stmt.execute("INSERT INTO users (name) VALUES ('Alice')");
            }

            // Verify user was created
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE name = 'Alice'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
        }
    }

    @Test
    @ContainerConfig(name = "database", injectAsParameter = true)
    void testFindUser(PostgreSQLContainer<?> db) throws Exception {
        // Reuses the same database container from the previous test
        assertThat(db).isNotNull();
        assertThat(db.isRunning()).isTrue();

        try (Connection conn = DriverManager.getConnection(
            db.getJdbcUrl(),
            db.getUsername(),
            db.getPassword()
        )) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS users (id SERIAL PRIMARY KEY, name VARCHAR(100))");
                stmt.execute("INSERT INTO users (name) VALUES ('Bob')");
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT name FROM users WHERE name = 'Bob'")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("Bob");
            }
        }
    }

    @Test
    @ContainerConfig(name = "database", injectAsParameter = true)
    void testDatabaseConfiguration(PostgreSQLContainer<?> db) {
        // Verify the database is configured correctly
        assertThat(db.getDatabaseName()).isEqualTo("testdb");
        assertThat(db.getUsername()).isEqualTo("testuser");
        assertThat(db.getPassword()).isEqualTo("testpass");
        assertThat(db.getJdbcUrl()).contains("jdbc:postgresql://");
    }
}

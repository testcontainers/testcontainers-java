package org.testcontainers.junit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotEquals;

public class SimplePostgreSQLTest {

    @Test
    public void testSimple() throws SQLException {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer()) {
            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testCommandOverride() throws SQLException {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer<>().withCommand("postgres -c max_connections=42")) {
            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT current_setting('max_connections')");
            String result = resultSet.getString(1);
            assertEquals("max_connections should be overriden", "42", result);
        }
    }

    @Test
    public void testUnsetCommand() throws SQLException {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer<>().withCommand("postgres -c max_connections=42").withCommand()) {
            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT current_setting('max_connections')");
            String result = resultSet.getString(1);
            assertNotEquals("max_connections should not be overriden", "42", result);
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (PostgreSQLContainer postgres = new PostgreSQLContainer<>().withInitScript("somepath/init_postgresql.sql")) {
            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT foo FROM bar");

            String firstColumnValue = resultSet.getString(1);
            assertEquals("Value from init script should equal real value", "hello world", firstColumnValue);
        }
    }

    private ResultSet performQuery(JdbcDatabaseContainer container, String sql) throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(container.getDriverClassName());
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());

        HikariDataSource ds = new HikariDataSource(hikariConfig);
        Statement statement = ds.getConnection().createStatement();
        statement.execute(sql);
        ResultSet resultSet = statement.getResultSet();
        resultSet.next();
        return resultSet;
    }
}

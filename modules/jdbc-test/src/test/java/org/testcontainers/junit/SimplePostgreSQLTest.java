package org.testcontainers.junit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.NonNull;
import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotEquals;

/**
 * @author richardnorth
 */
public class SimplePostgreSQLTest {
    @Test
    public void testSimple() throws SQLException {
        PostgreSQLContainer postgres = new PostgreSQLContainer();
        postgres.start();

        try {
            ResultSet resultSet = performQuery(postgres,"SELECT 1");
            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        } finally {
            postgres.stop();
        }
    }

    @Test
    public void testCommandOverride() throws SQLException {
        PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer().withCommand("postgres -c max_connections=42");
        postgres.start();

        try {
            ResultSet resultSet = performQuery(postgres,"SELECT current_setting('max_connections')");
            String result = resultSet.getString(1);
            assertEquals("max_connections should be overriden", "42", result);
        } finally {
            postgres.stop();
        }
    }

    @Test
    public void testUnsetCommand() throws SQLException {
        PostgreSQLContainer postgres = (PostgreSQLContainer) new PostgreSQLContainer().withCommand("postgres -c max_connections=42").withCommand();
        postgres.start();

        try {
            ResultSet resultSet = performQuery(postgres,"SELECT current_setting('max_connections')");
            String result = resultSet.getString(1);
            assertNotEquals("max_connections should not be overriden", "42", result);
        } finally {
            postgres.stop();
        }
    }

    @NonNull
    protected ResultSet performQuery(PostgreSQLContainer postgres, String sql) throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(postgres.getDriverClassName());
        hikariConfig.setJdbcUrl(postgres.getJdbcUrl());
        hikariConfig.setUsername(postgres.getUsername());
        hikariConfig.setPassword(postgres.getPassword());

        HikariDataSource ds = new HikariDataSource(hikariConfig);
        Statement statement = ds.getConnection().createStatement();
        statement.execute(sql);
        ResultSet resultSet = statement.getResultSet();

        resultSet.next();
        return resultSet;
    }
}

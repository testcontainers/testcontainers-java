package org.rnorth.testcontainers.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class JDBCDriverTest {

    @Test
    public void testMySQLWithVersion() throws SQLException {
        performTest("jdbc:tc:mysql:5.5.43://hostname/databasename");
    }

    @Test
    public void testMySQLWithNoSpecifiedVersion() throws SQLException {
        performTest("jdbc:tc:mysql://hostname/databasename");
    }

    @Test
    public void testPostgreSQLWithNoSpecifiedVersion() throws SQLException {
        performTest("jdbc:tc:postgresql://hostname/databasename");
    }

    private void performTest(String jdbcUrl) throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setConnectionTestQuery("SELECT 1");

        HikariDataSource ds = new HikariDataSource(hikariConfig);
        Statement statement = ds.getConnection().createStatement();
        statement.execute("SELECT 1");
        ResultSet resultSet = statement.getResultSet();

        resultSet.next();
        int resultSetInt = resultSet.getInt(1);
        assertEquals(1, resultSetInt);
    }
}

package org.rnorth.testcontainers.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class JDBCDriverTest {

    @Test
    public void testMySQLWithVersion() throws SQLException {
        performSimpleTest("jdbc:tc:mysql:5.5.43://hostname/databasename");
    }

    @Test
    public void testMySQLWithNoSpecifiedVersion() throws SQLException {
        performSimpleTest("jdbc:tc:mysql://hostname/databasename");
    }

    @Test
    public void testPostgreSQLWithNoSpecifiedVersion() throws SQLException {
        performSimpleTest("jdbc:tc:postgresql://hostname/databasename");
    }

    @Test
    public void testMySQLWithClasspathInitScript() throws SQLException {
        // Separate JDBC connections => new container for each of the below
        performSimpleTest("jdbc:tc:mysql://hostname/databasename?TC_INITSCRIPT=somepath/init_mysql.sql");

        performTestForScriptedSchema("jdbc:tc:mysql://hostname/databasename?TC_INITSCRIPT=somepath/init_mysql.sql");
    }

    @Test
    public void testMySQLWithClasspathInitFunction() throws SQLException {
        // Separate JDBC connections => new container for each of the below
        performSimpleTest("jdbc:tc:mysql://hostname/databasename?TC_INITFUNCTION=org.rnorth.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction");

        performTestForScriptedSchema("jdbc:tc:mysql://hostname/databasename?TC_INITFUNCTION=org.rnorth.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction");
    }

    private void performTestForScriptedSchema(String jdbcUrl) throws SQLException {
        getQueryRunner(jdbcUrl).query("SELECT foo FROM bar", rs -> {
            rs.next();
            String resultSetString = rs.getString(1);
            assertEquals("hello world", resultSetString);
            return true;
        });
    }

    private void performSimpleTest(String jdbcUrl) throws SQLException {
        getQueryRunner(jdbcUrl).query("SELECT 1", rs -> {
            rs.next();
            int resultSetInt = rs.getInt(1);
            assertEquals(1, resultSetInt);
            return true;
        });
    }

    private QueryRunner getQueryRunner(String jdbcUrl) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setMaximumPoolSize(1);

        HikariDataSource ds = new HikariDataSource(hikariConfig);

        return new QueryRunner(ds);
    }

    public static void sampleInitFunction(Connection connection) throws SQLException {
        connection.createStatement().execute("CREATE TABLE bar (\n" +
                "  foo VARCHAR(255)\n" +
                ");");
        connection.createStatement().execute("INSERT INTO bar (foo) VALUES ('hello world');");
    }
}

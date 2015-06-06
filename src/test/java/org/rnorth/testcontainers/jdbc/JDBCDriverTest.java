package org.rnorth.testcontainers.jdbc;

import com.google.common.hash.HashCode;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.junit.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class JDBCDriverTest {

    private static int initFunctionInvocationCount = 0;
    private static String sampleSchemaHash = HashCode.fromLong(new Random().nextLong()).toString();

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
        performSimpleTest("jdbc:tc:mysql://hostname/databasename?TC_INITSCRIPT=somepath/init_mysql.sql");

        performTestForScriptedSchema("jdbc:tc:mysql://hostname/databasename?TC_INITSCRIPT=somepath/init_mysql.sql");
    }

    @Test
    public void testMySQLWithClasspathInitFunction() throws SQLException {
        performSimpleTest("jdbc:tc:mysql://hostname/databasename?TC_INITFUNCTION=org.rnorth.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction");

        performTestForScriptedSchema("jdbc:tc:mysql://hostname/databasename?TC_INITFUNCTION=org.rnorth.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction");
    }

    @Test
    public void testMySQLWithClasspathInitFunctionAndInitVersioning() throws SQLException {
        HikariDataSource dataSource;

        int initFunctionInvocationCountBefore = initFunctionInvocationCount;
        dataSource = getDataSource("jdbc:tc:mysql://hostname/databasename?TC_INITFUNCTION=org.rnorth.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction&TC_TAGFUNCTION=org.rnorth.testcontainers.jdbc.JDBCDriverTest::sampleTagFunction", 10);
        dataSource.close();

        int initFunctionInvocationCountAfter = initFunctionInvocationCount;
        dataSource = getDataSource("jdbc:tc:mysql://hostname/databasename?TC_INITFUNCTION=org.rnorth.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction&TC_TAGFUNCTION=org.rnorth.testcontainers.jdbc.JDBCDriverTest::sampleTagFunction", 10);
        dataSource.close();

        assertEquals("The invocation function isn't run the second time if the tag function returns a constant value", initFunctionInvocationCountBefore, initFunctionInvocationCountAfter);
    }

    @Test
    public void testMySQLWithConnectionPoolUsingSameContainer() throws SQLException {
        HikariDataSource dataSource = getDataSource("jdbc:tc:mysql://hostname/databasename?TC_INITFUNCTION=org.rnorth.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction", 10);
        for (int i = 0; i < 100; i++) {
            new QueryRunner(dataSource).insert("INSERT INTO my_counter (n) VALUES (5)", rs -> true);
        }

        new QueryRunner(dataSource).query("SELECT COUNT(1) FROM my_counter", rs -> {
            rs.next();
            int resultSetInt = rs.getInt(1);
            assertEquals(100, resultSetInt);
            return true;
        });

        new QueryRunner(dataSource).query("SELECT SUM(n) FROM my_counter", rs -> {
            rs.next();
            int resultSetInt = rs.getInt(1);
            assertEquals(500, resultSetInt);
            return true;
        });
    }

    @Test
    public void testMySQLWithQueryParams() throws SQLException {
        performSimpleTestWithCharacterSet("jdbc:tc:mysql://hostname/databasename?useUnicode=yes&characterEncoding=utf8");
    }

    private void performSimpleTest(String jdbcUrl) throws SQLException {
        HikariDataSource dataSource = getDataSource(jdbcUrl, 1);
        new QueryRunner(dataSource).query("SELECT 1", rs -> {
            rs.next();
            int resultSetInt = rs.getInt(1);
            assertEquals(1, resultSetInt);
            return true;
        });
        dataSource.close();
    }

    private void performTestForScriptedSchema(String jdbcUrl) throws SQLException {
        HikariDataSource dataSource = getDataSource(jdbcUrl, 1);
        new QueryRunner(dataSource).query("SELECT foo FROM bar WHERE foo LIKE '%world'", rs -> {
            rs.next();
            String resultSetString = rs.getString(1);
            assertEquals("hello world", resultSetString);
            return true;
        });
        dataSource.close();
    }

    private void performSimpleTestWithCharacterSet(String jdbcUrl) throws SQLException {
        HikariDataSource dataSource = getDataSource(jdbcUrl, 1);
        new QueryRunner(dataSource).query("SHOW VARIABLES LIKE 'character\\_set\\_connection'", rs -> {
            rs.next();
            String resultSetInt = rs.getString(2);
            assertEquals("utf8", resultSetInt);
            return true;
        });
        dataSource.close();
    }

    private HikariDataSource getDataSource(String jdbcUrl, int poolSize) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(poolSize);

        return new HikariDataSource(hikariConfig);
    }

    public static void sampleInitFunction(Connection connection) throws SQLException {
        connection.createStatement().execute("CREATE TABLE bar (\n" +
                "  foo VARCHAR(255)\n" +
                ");");
        connection.createStatement().execute("INSERT INTO bar (foo) VALUES ('hello world');");
        connection.createStatement().execute("CREATE TABLE my_counter (\n" +
                "  n INT\n" +
                ");");
        initFunctionInvocationCount++;
    }

    public static String sampleTagFunction(Connection connection) throws SQLException {
        return "%s-" + sampleSchemaHash;
    }
}

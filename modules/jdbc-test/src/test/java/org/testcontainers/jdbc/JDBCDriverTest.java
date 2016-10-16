package org.testcontainers.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang.SystemUtils;
import org.junit.After;
import org.junit.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assume.assumeFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

/**
 *
 */
public class JDBCDriverTest {

    @After
    public void testCleanup() {
        ContainerDatabaseDriver.killContainers();
    }

    @Test
    public void testMySQLWithVersion() throws SQLException {
        performSimpleTest("jdbc:tc:mysql:5.5.43://hostname/databasename");
    }

    @Test
    public void testMySQLWithNoSpecifiedVersion() throws SQLException {
        performSimpleTest("jdbc:tc:mysql://hostname/databasename");
    }

    @Test
    public void testMySQLWithCustomIniFile() throws SQLException {
        assumeFalse(SystemUtils.IS_OS_WINDOWS);
        HikariDataSource ds = getDataSource("jdbc:tc:mysql:5.6://hostname/databasename?TC_MY_CNF=somepath/mysql_conf_override", 1);
        Statement statement = ds.getConnection().createStatement();
        statement.execute("SELECT @@GLOBAL.innodb_file_format");
        ResultSet resultSet = statement.getResultSet();

        resultSet.next();
        String result = resultSet.getString(1);

        assertEquals("The InnoDB file format has been set by the ini file content", "Barracuda", result);
    }

    @Test
    public void testMySQLWithClasspathInitScript() throws SQLException {
        performSimpleTest("jdbc:tc:mysql://hostname/databasename?TC_INITSCRIPT=somepath/init_mysql.sql");

        performTestForScriptedSchema("jdbc:tc:mysql://hostname/databasename?TC_INITSCRIPT=somepath/init_mysql.sql");
    }

    @Test
    public void testMySQLWithClasspathInitFunction() throws SQLException {
        performSimpleTest("jdbc:tc:mysql://hostname/databasename?TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction");

        performTestForScriptedSchema("jdbc:tc:mysql://hostname/databasename?TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction");
    }

    @Test
    public void testMySQLWithQueryParams() throws SQLException {
        performSimpleTestWithCharacterSet("jdbc:tc:mysql://hostname/databasename?useUnicode=yes&characterEncoding=utf8");
    }

    @Test
    public void testPostgreSQLWithNoSpecifiedVersion() throws SQLException {
        performSimpleTest("jdbc:tc:postgresql://hostname/databasename");
    }


    @Test
    public void testMariaDBWithVersion() throws SQLException {
        performSimpleTest("jdbc:tc:mariadb:10.1.16://hostname/databasename");
    }

    @Test
    public void testMariaDBWithNoSpecifiedVersion() throws SQLException {
        performSimpleTest("jdbc:tc:mariadb://hostname/databasename");
    }

    @Test
    public void testMariaDBWithCustomIniFile() throws SQLException {
        assumeFalse(SystemUtils.IS_OS_WINDOWS);
        HikariDataSource ds = getDataSource("jdbc:tc:mariadb:10.1.16://hostname/databasename?TC_MY_CNF=somepath/mariadb_conf_override", 1);
        Statement statement = ds.getConnection().createStatement();
        statement.execute("SELECT @@GLOBAL.innodb_file_format");
        ResultSet resultSet = statement.getResultSet();

        resultSet.next();
        String result = resultSet.getString(1);

        assertEquals("The InnoDB file format has been set by the ini file content", "Barracuda", result);
    }

    @Test
    public void testMariaDBWithClasspathInitScript() throws SQLException {
        performSimpleTest("jdbc:tc:mariadb://hostname/databasename?TC_INITSCRIPT=somepath/init_mariadb.sql");

        performTestForScriptedSchema("jdbc:tc:mariadb://hostname/databasename?TC_INITSCRIPT=somepath/init_mariadb.sql");
    }

    @Test
    public void testMariaDBWithClasspathInitFunction() throws SQLException {
        performSimpleTest("jdbc:tc:mariadb://hostname/databasename?TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction");

        performTestForScriptedSchema("jdbc:tc:mariadb://hostname/databasename?TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction");
    }

    @Test
    public void testMariaDBWithQueryParams() throws SQLException {
        performSimpleTestWithCharacterSet("jdbc:tc:mariadb://hostname/databasename?useUnicode=yes&characterEncoding=utf8");
    }

    private void performSimpleTest(String jdbcUrl) throws SQLException {
        HikariDataSource dataSource = getDataSource(jdbcUrl, 1);
        new QueryRunner(dataSource).query("SELECT 1", new ResultSetHandler<Object>() {
            @Override
            public Object handle(ResultSet rs) throws SQLException {
                rs.next();
                int resultSetInt = rs.getInt(1);
                assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
                return true;
            }
        });
        dataSource.close();
    }

    private void performTestForScriptedSchema(String jdbcUrl) throws SQLException {
        HikariDataSource dataSource = getDataSource(jdbcUrl, 1);
        new QueryRunner(dataSource).query("SELECT foo FROM bar WHERE foo LIKE '%world'", new ResultSetHandler<Object>() {
            @Override
            public Object handle(ResultSet rs) throws SQLException {
                rs.next();
                String resultSetString = rs.getString(1);
                assertEquals("A basic SELECT query succeeds where the schema has been applied from a script", "hello world", resultSetString);
                return true;
            }
        });
        dataSource.close();
    }

    private void performSimpleTestWithCharacterSet(String jdbcUrl) throws SQLException {
        HikariDataSource dataSource = getDataSource(jdbcUrl, 1);
        new QueryRunner(dataSource).query("SHOW VARIABLES LIKE 'character\\_set\\_connection'", new ResultSetHandler<Object>() {
            @Override
            public Object handle(ResultSet rs) throws SQLException {
                rs.next();
                String resultSetInt = rs.getString(2);
                assertEquals("Passing query parameters to set DB connection encoding is successful", "utf8", resultSetInt);
                return true;
            }
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
    }
}

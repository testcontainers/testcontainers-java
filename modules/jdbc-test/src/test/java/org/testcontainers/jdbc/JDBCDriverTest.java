package org.testcontainers.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.lang.SystemUtils;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static java.util.Arrays.asList;
import static org.junit.Assume.assumeFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

@RunWith(Parameterized.class)
public class JDBCDriverTest {

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Iterable<Object[]> data() {
        return asList(
                new Object[][]{
                        {"jdbc:tc:mysql:5.5.43://hostname/databasename", false, false, false},
                        {"jdbc:tc:mysql://hostname/databasename?TC_INITSCRIPT=somepath/init_mysql.sql", true, false, false},
                        {"jdbc:tc:mysql://hostname/databasename?TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction", true, false, false},
                        {"jdbc:tc:mysql://hostname/databasename?useUnicode=yes&characterEncoding=utf8", false, true, false},
                        {"jdbc:tc:mysql://hostname/databasename", false, false, false},
                        {"jdbc:tc:postgresql://hostname/databasename", false, false, false},
                        {"jdbc:tc:mariadb:10.1.16://hostname/databasename", false, false, false},
                        {"jdbc:tc:mariadb://hostname/databasename", false, false, false},
                        {"jdbc:tc:mariadb://hostname/databasename?useUnicode=yes&characterEncoding=utf8", false, true, false},
                        {"jdbc:tc:mariadb://hostname/databasename?TC_INITSCRIPT=somepath/init_mariadb.sql", true, false, false},
                        {"jdbc:tc:mariadb://hostname/databasename?TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction", true, false, false},
                        {"jdbc:tc:mysql:5.6://hostname/databasename?TC_MY_CNF=somepath/mysql_conf_override", false, false, true},
                        {"jdbc:tc:mariadb:10.1.16://hostname/databasename?TC_MY_CNF=somepath/mariadb_conf_override", false, false, true}
                });
    }

    @Parameterized.Parameter(0)
    public String jdbcUrl;

    @Parameterized.Parameter(1)
    public boolean performTestForScriptedSchema;

    @Parameterized.Parameter(2)
    public boolean performTestForCharacterSet;

    @Parameterized.Parameter(3)
    public boolean performTestForCustomIniFile;

    @Test
    public void test() throws SQLException {
        performSimpleTest(jdbcUrl);

        if (performTestForScriptedSchema) {
            performTestForScriptedSchema(jdbcUrl);
        }

        if (performTestForCharacterSet) {
            performSimpleTestWithCharacterSet(jdbcUrl);
        }

        if (performTestForCustomIniFile) {
            performTestForCustomIniFile(jdbcUrl);
        }
    }

    private void performSimpleTest(String jdbcUrl) throws SQLException {
        HikariDataSource dataSource = getDataSource(jdbcUrl, 1);
        new QueryRunner(dataSource).query("SELECT 1", (ResultSetHandler<Object>) rs -> {
            rs.next();
            int resultSetInt = rs.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
            return true;
        });
        dataSource.close();
    }

    private void performTestForScriptedSchema(String jdbcUrl) throws SQLException {
        HikariDataSource dataSource = getDataSource(jdbcUrl, 1);
        new QueryRunner(dataSource).query("SELECT foo FROM bar WHERE foo LIKE '%world'", (ResultSetHandler<Object>) rs -> {
            rs.next();
            String resultSetString = rs.getString(1);
            assertEquals("A basic SELECT query succeeds where the schema has been applied from a script", "hello world", resultSetString);
            return true;
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

    private void performTestForCustomIniFile(final String jdbcUrl) throws SQLException {
        assumeFalse(SystemUtils.IS_OS_WINDOWS);
        HikariDataSource ds = getDataSource(jdbcUrl, 1);
        Statement statement = ds.getConnection().createStatement();
        statement.execute("SELECT @@GLOBAL.innodb_file_format");
        ResultSet resultSet = statement.getResultSet();

        resultSet.next();
        String result = resultSet.getString(1);

        assertEquals("The InnoDB file format has been set by the ini file content", "Barracuda", result);
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

    @AfterClass
    public static void testCleanup() {
        ContainerDatabaseDriver.killContainers();
    }
}

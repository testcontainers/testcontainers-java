package org.testcontainers.jdbc;

import com.googlecode.junittoolbox.ParallelParameterized;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang.SystemUtils;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;

import static java.util.Arrays.asList;
import static org.junit.Assume.assumeFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

@RunWith(ParallelParameterized.class)
public class JDBCDriverTest {

    private enum Options {
        ScriptedSchema,
        CharacterSet,
        CustomIniFile,
        JDBCParams,
        PmdKnownBroken
    }

    @Parameter
    public String jdbcUrl;
    @Parameter(1)
    public EnumSet<Options> options;

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Iterable<Object[]> data() {
        return asList(
            new Object[][]{
                {"jdbc:tc:mysql://hostname/databasename", EnumSet.noneOf(Options.class)},
                {"jdbc:tc:mysql://hostname/databasename?user=someuser&TC_INITSCRIPT=somepath/init_mysql.sql", EnumSet.of(Options.ScriptedSchema, Options.JDBCParams)},
                {"jdbc:tc:mysql:5.5.43://hostname/databasename?user=someuser&TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction", EnumSet.of(Options.ScriptedSchema, Options.JDBCParams)},
                {"jdbc:tc:mysql:5.5.43://hostname/databasename?user=someuser&password=somepwd&TC_INITSCRIPT=somepath/init_mysql.sql", EnumSet.of(Options.ScriptedSchema, Options.JDBCParams)},
                {"jdbc:tc:mysql:5.5.43://hostname/databasename?user=someuser&password=somepwd&TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction", EnumSet.of(Options.ScriptedSchema, Options.JDBCParams)},
                {"jdbc:tc:mysql:5.5.43://hostname/databasename?TC_INITSCRIPT=somepath/init_unicode_mysql.sql&useUnicode=yes&characterEncoding=utf8", EnumSet.of(Options.CharacterSet)},
                {"jdbc:tc:mysql:5.5.43://hostname/databasename", EnumSet.noneOf(Options.class)},
                {"jdbc:tc:mysql:5.5.43://hostname/databasename?useSSL=false", EnumSet.noneOf(Options.class)},
                {"jdbc:tc:postgresql:9.6.8://hostname/databasename", EnumSet.noneOf(Options.class)},
                {"jdbc:tc:postgis://hostname/databasename", EnumSet.noneOf(Options.class)},
                {"jdbc:tc:postgis:9.6://hostname/databasename", EnumSet.noneOf(Options.class)},
                {"jdbc:tc:mysql:5.6://hostname/databasename?TC_MY_CNF=somepath/mysql_conf_override", EnumSet.of(Options.CustomIniFile)},
                {"jdbc:tc:mariadb://hostname/databasename", EnumSet.noneOf(Options.class)},
                {"jdbc:tc:mariadb:10.2.14://hostname/databasename", EnumSet.noneOf(Options.class)},
                {"jdbc:tc:mariadb:10.2.14://hostname/databasename?TC_INITSCRIPT=somepath/init_unicode_mysql.sql&useUnicode=yes&characterEncoding=utf8", EnumSet.of(Options.CharacterSet)},
                {"jdbc:tc:mariadb:10.2.14://hostname/databasename?TC_INITSCRIPT=somepath/init_mariadb.sql", EnumSet.of(Options.ScriptedSchema)},
                {"jdbc:tc:mariadb:10.2.14://hostname/databasename?TC_INITFUNCTION=org.testcontainers.jdbc.JDBCDriverTest::sampleInitFunction", EnumSet.of(Options.ScriptedSchema)},
                {"jdbc:tc:mariadb:10.2.14://hostname/databasename?TC_MY_CNF=somepath/mariadb_conf_override", EnumSet.of(Options.CustomIniFile)},
                {"jdbc:tc:clickhouse://hostname/databasename", EnumSet.of(Options.PmdKnownBroken)},
            });
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

    @Test
    public void test() throws SQLException {
        performSimpleTest(jdbcUrl);

        if (options.contains(Options.ScriptedSchema)) {
            performTestForScriptedSchema(jdbcUrl);
        }

        if (options.contains(Options.JDBCParams)) {
            performTestForJDBCParamUsage(jdbcUrl);
        }

        if (options.contains(Options.CharacterSet)) {
            //Called twice to ensure that the query string parameters are used when
            //connections are created from cached containers.
            performSimpleTestWithCharacterSet(jdbcUrl);
            performSimpleTestWithCharacterSet(jdbcUrl);

            performTestForCharacterEncodingForInitialScriptConnection(jdbcUrl);
        }

        if (options.contains(Options.CustomIniFile)) {
            performTestForCustomIniFile(jdbcUrl);
        }
    }

    private void performSimpleTest(String jdbcUrl) throws SQLException {
        try (HikariDataSource dataSource = getDataSource(jdbcUrl, 1)) {
            boolean result = new QueryRunner(dataSource, options.contains(Options.PmdKnownBroken)).query("SELECT 1", rs -> {
                rs.next();
                int resultSetInt = rs.getInt(1);
                assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
                return true;
            });

            assertTrue("The database returned a record as expected", result);
        }
    }

    private void performTestForScriptedSchema(String jdbcUrl) throws SQLException {
        try (HikariDataSource dataSource = getDataSource(jdbcUrl, 1)) {
            boolean result = new QueryRunner(dataSource).query("SELECT foo FROM bar WHERE foo LIKE '%world'", rs -> {
                rs.next();
                String resultSetString = rs.getString(1);
                assertEquals("A basic SELECT query succeeds where the schema has been applied from a script", "hello world", resultSetString);
                return true;
            });
        }
    }

    private void performTestForJDBCParamUsage(String jdbcUrl) throws SQLException {
        try (HikariDataSource dataSource = getDataSource(jdbcUrl, 1)) {
            boolean result = new QueryRunner(dataSource).query("select CURRENT_USER()", rs -> {
                rs.next();
                String resultUser = rs.getString(1);
                assertEquals("User from query param is created.", "someuser@%", resultUser);
                return true;
            });

            assertTrue("The database returned a record as expected", result);

            result = new QueryRunner(dataSource).query("SELECT DATABASE()", rs -> {
                rs.next();
                String resultDB = rs.getString(1);
                assertEquals("Database name from URL String is used.", "databasename", resultDB);
                return true;
            });

            assertTrue("The database returned a record as expected", result);
        }
    }

    private void performTestForCharacterEncodingForInitialScriptConnection(String jdbcUrl) throws SQLException {
        try (HikariDataSource dataSource = getDataSource(jdbcUrl, 1)) {
            boolean result = new QueryRunner(dataSource).query("SELECT foo FROM bar WHERE foo LIKE '%мир'", rs -> {
                rs.next();
                String resultSetString = rs.getString(1);
                assertEquals("A SELECT query succeed and the correct charset has been applied for the init script", "привет мир", resultSetString);
                return true;
            });

            assertTrue("The database returned a record as expected", result);
        }
    }

    /**
     * This method intentionally verifies encoding twice to ensure that the query string parameters are used when
     * Connections are created from cached containers.
     *
     * @param jdbcUrl
     * @throws SQLException
     */
    private void performSimpleTestWithCharacterSet(String jdbcUrl) throws SQLException {
        HikariDataSource datasource1 = verifyCharacterSet(jdbcUrl);
        HikariDataSource datasource2 = verifyCharacterSet(jdbcUrl);
        datasource1.close();
        datasource2.close();
    }

    private HikariDataSource verifyCharacterSet(String jdbcUrl) throws SQLException {
        HikariDataSource dataSource = getDataSource(jdbcUrl, 1);
        boolean result = new QueryRunner(dataSource).query("SHOW VARIABLES LIKE 'character\\_set\\_connection'", rs -> {
            rs.next();
            String resultSetInt = rs.getString(2);
            assertEquals("Passing query parameters to set DB connection encoding is successful", "utf8", resultSetInt);
            return true;
        });

        assertTrue("The database returned a record as expected", result);
        return dataSource;
    }

    private void performTestForCustomIniFile(final String jdbcUrl) throws SQLException {
        assumeFalse(SystemUtils.IS_OS_WINDOWS);
        try (HikariDataSource ds = getDataSource(jdbcUrl, 1)) {
            Statement statement = ds.getConnection().createStatement();
            statement.execute("SELECT @@GLOBAL.innodb_file_format");
            ResultSet resultSet = statement.getResultSet();

            assertTrue("The query returns a result", resultSet.next());
            String result = resultSet.getString(1);

            assertEquals("The InnoDB file format has been set by the ini file content", "Barracuda", result);
        }
    }

    private HikariDataSource getDataSource(String jdbcUrl, int poolSize) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(poolSize);

        return new HikariDataSource(hikariConfig);
    }
}

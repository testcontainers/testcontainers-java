package org.testcontainers.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.lang.SystemUtils;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameter;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.EnumSet;

import static org.junit.Assume.assumeFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

public class AbstractJDBCDriverTest {

    protected enum Options {
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
        try (HikariDataSource dataSource = getDataSource(jdbcUrl, 1)) {
            performSimpleTest(dataSource);

            if (options.contains(Options.ScriptedSchema)) {
                performTestForScriptedSchema(dataSource);
            }

            if (options.contains(Options.JDBCParams)) {
                performTestForJDBCParamUsage(dataSource);
            }

            if (options.contains(Options.CharacterSet)) {
                performSimpleTestWithCharacterSet(jdbcUrl);

                performTestForCharacterEncodingForInitialScriptConnection(dataSource);
            }

            if (options.contains(Options.CustomIniFile)) {
                performTestForCustomIniFile(dataSource);
            }
        }
    }

    private void performSimpleTest(HikariDataSource dataSource) throws SQLException {
        String query = "SELECT 1";
        if (jdbcUrl.startsWith("jdbc:tc:db2:")) {
            query = "SELECT 1 FROM SYSIBM.SYSDUMMY1";
        }

        boolean result = new QueryRunner(dataSource, options.contains(Options.PmdKnownBroken)).query(query, rs -> {
            rs.next();
            int resultSetInt = rs.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
            return true;
        });

        assertTrue("The database returned a record as expected", result);
    }

    private void performTestForScriptedSchema(HikariDataSource dataSource) throws SQLException {
        boolean result = new QueryRunner(dataSource).query("SELECT foo FROM bar WHERE foo LIKE '%world'", rs -> {
            rs.next();
            String resultSetString = rs.getString(1);
            assertEquals("A basic SELECT query succeeds where the schema has been applied from a script", "hello world", resultSetString);
            return true;
        });

        assertTrue("The database returned a record as expected", result);
    }

    private void performTestForJDBCParamUsage(HikariDataSource dataSource) throws SQLException {
        boolean result = new QueryRunner(dataSource).query("select CURRENT_USER", rs -> {
            rs.next();
            String resultUser = rs.getString(1);
            // Not all databases (eg. Postgres) return @% at the end of user name. We just need to make sure the user name matches.
            if (resultUser.endsWith("@%")) {
                resultUser = resultUser.substring(0, resultUser.length() - 2);
            }
            assertEquals("User from query param is created.", "someuser", resultUser);
            return true;
        });

        assertTrue("The database returned a record as expected", result);

        String databaseQuery = "SELECT DATABASE()";
        // Postgres does not have Database() as a function
        String databaseType = ConnectionUrl.newInstance(jdbcUrl).getDatabaseType();
        if (databaseType.equalsIgnoreCase("postgresql") ||
            databaseType.equalsIgnoreCase("postgis") ||
            databaseType.equalsIgnoreCase("timescaledb")) {
            databaseQuery = "SELECT CURRENT_DATABASE()";
        }

        result = new QueryRunner(dataSource).query(databaseQuery, rs -> {
            rs.next();
            String resultDB = rs.getString(1);
            assertEquals("Database name from URL String is used.", "databasename", resultDB);
            return true;
        });

        assertTrue("The database returned a record as expected", result);
    }

    private void performTestForCharacterEncodingForInitialScriptConnection(HikariDataSource dataSource) throws SQLException {
        boolean result = new QueryRunner(dataSource).query("SELECT foo FROM bar WHERE foo LIKE '%мир'", rs -> {
            rs.next();
            String resultSetString = rs.getString(1);
            assertEquals("A SELECT query succeed and the correct charset has been applied for the init script", "привет мир", resultSetString);
            return true;
        });

        assertTrue("The database returned a record as expected", result);
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
            String resultSetString = rs.getString(2);
            assertTrue("Passing query parameters to set DB connection encoding is successful", resultSetString.startsWith("utf8"));
            return true;
        });

        assertTrue("The database returned a record as expected", result);
        return dataSource;
    }

    private void performTestForCustomIniFile(HikariDataSource dataSource) throws SQLException {
        assumeFalse(SystemUtils.IS_OS_WINDOWS);
        Statement statement = dataSource.getConnection().createStatement();
        statement.execute("SELECT @@GLOBAL.innodb_file_format");
        ResultSet resultSet = statement.getResultSet();

        assertTrue("The query returns a result", resultSet.next());
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
}

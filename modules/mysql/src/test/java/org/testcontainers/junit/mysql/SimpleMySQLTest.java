package org.testcontainers.junit.mysql;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.rnorth.visibleassertions.VisibleAssertions.fail;
import static org.testcontainers.MySQLTestImages.MYSQL_56_IMAGE;
import static org.testcontainers.MySQLTestImages.MYSQL_57_IMAGE;


public class SimpleMySQLTest extends AbstractContainerDatabaseTest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleMySQLTest.class);

    /*
     * Ordinarily you wouldn't try and run multiple containers simultaneously - this is just used for testing.
     * To avoid memory issues with the default, low memory, docker machine setup, we instantiate only one container
     * at a time, inside the test methods themselves.
     */
    /*
    @ClassRule
    public static MySQLContainer<?> mysql = new MySQLContainer<>(MYSQL_IMAGE);

    @ClassRule
    public static MySQLContainer<?> mysqlOldVersion = new MySQLContainer<>(DockerImageName.parse("mysql:5.5");)

    @ClassRule
    public static MySQLContainer<?> mysqlCustomConfig = new MySQLContainer<>(DockerImageName.parse("mysql:5.6"))
                                                            .withConfigurationOverride("somepath/mysql_conf_override");
    */

    @Test
    public void testSimple() throws SQLException {
        try (MySQLContainer<?> mysql = new MySQLContainer<>(MYSQL_57_IMAGE)
            .withConfigurationOverride("somepath/mysql_conf_override")
            .withLogConsumer(new Slf4jLogConsumer(logger))) {

            mysql.start();

            ResultSet resultSet = performQuery(mysql, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);

            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testSpecificVersion() throws SQLException {
        try (MySQLContainer<?> mysqlOldVersion = new MySQLContainer<>(MYSQL_56_IMAGE)
            .withConfigurationOverride("somepath/mysql_conf_override")
            .withLogConsumer(new Slf4jLogConsumer(logger))) {

            mysqlOldVersion.start();

            ResultSet resultSet = performQuery(mysqlOldVersion, "SELECT VERSION()");
            String resultSetString = resultSet.getString(1);

            assertTrue("The database version can be set using a container rule parameter", resultSetString.startsWith("5.6"));
        }
    }

    @Test
    public void testMySQLWithCustomIniFile() throws SQLException {
        assumeFalse(SystemUtils.IS_OS_WINDOWS);

        try (MySQLContainer<?> mysqlCustomConfig = new MySQLContainer<>(MYSQL_56_IMAGE)
            .withConfigurationOverride("somepath/mysql_conf_override")) {

            mysqlCustomConfig.start();

            ResultSet resultSet = performQuery(mysqlCustomConfig, "SELECT @@GLOBAL.innodb_file_format");
            String result = resultSet.getString(1);

            assertEquals("The InnoDB file format has been set by the ini file content", "Barracuda", result);
        }
    }

    @Test
    public void testCommandOverride() throws SQLException {
        try (MySQLContainer<?> mysqlCustomConfig = new MySQLContainer<>(MYSQL_57_IMAGE)
            .withCommand("mysqld --auto_increment_increment=42")) {

            mysqlCustomConfig.start();

            ResultSet resultSet = performQuery(mysqlCustomConfig, "show variables like 'auto_increment_increment'");
            String result = resultSet.getString("Value");

            assertEquals("Auto increment increment should be overriden by command line", "42", result);
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (MySQLContainer<?> container = new MySQLContainer<>(MYSQL_57_IMAGE)
            .withInitScript("somepath/init_mysql.sql")
            .withLogConsumer(new Slf4jLogConsumer(logger))) {
            container.start();

            ResultSet resultSet = performQuery(container, "SELECT foo FROM bar");
            String firstColumnValue = resultSet.getString(1);

            assertEquals("Value from init script should equal real value", "hello world", firstColumnValue);
        }
    }

    @Test(expected = ContainerLaunchException.class)
    public void testEmptyPasswordWithNonRootUser() {
        try (MySQLContainer<?> container = new MySQLContainer<>(MYSQL_56_IMAGE)
                    .withDatabaseName("TEST")
                    .withUsername("test")
                    .withPassword("")
                    .withEnv("MYSQL_ROOT_HOST", "%")){
            container.start();
            fail("ContainerLaunchException expected to be thrown");
        }
    }

    @Test
    public void testEmptyPasswordWithRootUser() throws SQLException {
        // Add MYSQL_ROOT_HOST environment so that we can root login from anywhere for testing purposes
        try (MySQLContainer<?> mysql = new MySQLContainer<>(MYSQL_56_IMAGE)
            .withDatabaseName("foo")
            .withUsername("root")
            .withPassword("")
            .withEnv("MYSQL_ROOT_HOST", "%")) {

            mysql.start();

            ResultSet resultSet = performQuery(mysql, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);

            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testWithAdditionalUrlParamTimeZone() throws SQLException {
        MySQLContainer<?> mysql = new MySQLContainer<>(MYSQL_57_IMAGE)
            .withUrlParam("serverTimezone", "Europe/Zurich")
            .withEnv("TZ", "Europe/Zurich")
            .withLogConsumer(new Slf4jLogConsumer(logger));
        mysql.start();

        try(Connection connection = mysql.createConnection("")) {
            Statement statement = connection.createStatement();
            statement.execute("SELECT NOW();");
            try (ResultSet resultSet = statement.getResultSet()) {
                resultSet.next();

                // checking that the time_zone MySQL is Europe/Zurich
                LocalDateTime localDateTime = resultSet.getObject(1, LocalDateTime.class);
                ZonedDateTime actualDateTime = localDateTime.atZone(ZoneId.of("Europe/Zurich"))
                    .truncatedTo(ChronoUnit.MINUTES);
                ZonedDateTime expectedDateTime = ZonedDateTime.now(ZoneId.of("Europe/Zurich"))
                    .truncatedTo(ChronoUnit.MINUTES);

                String message = String.format("MySQL time zone is not Europe/Zurich. MySQL date:%s, current date:%s",
                    actualDateTime, expectedDateTime);
                assertTrue(message, actualDateTime.equals(expectedDateTime));
            }
        } finally {
            mysql.stop();
        }
    }

    @Test
    public void testWithAdditionalUrlParamMultiQueries() throws SQLException {
        MySQLContainer<?> mysql = new MySQLContainer<>(MYSQL_57_IMAGE)
            .withUrlParam("allowMultiQueries", "true")
            .withLogConsumer(new Slf4jLogConsumer(logger));
        mysql.start();

        try(Connection connection = mysql.createConnection("")) {
            Statement statement = connection.createStatement();
            String multiQuery = "DROP TABLE IF EXISTS bar; " +
                "CREATE TABLE bar (foo VARCHAR(20)); " +
                "INSERT INTO bar (foo) VALUES ('hello world');";
            statement.execute(multiQuery);
            statement.execute("SELECT foo FROM bar;");
            try(ResultSet resultSet = statement.getResultSet()) {
                resultSet.next();
                String firstColumnValue = resultSet.getString(1);
                assertEquals("Value from bar should equal real value", "hello world", firstColumnValue);
            }
        } finally {
            mysql.stop();
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        MySQLContainer<?> mysql = new MySQLContainer<>(MYSQL_57_IMAGE)
            .withUrlParam("allowMultiQueries", "true")
            .withUrlParam("rewriteBatchedStatements", "true")
            .withLogConsumer(new Slf4jLogConsumer(logger));

        try {
            mysql.start();
            String jdbcUrl = mysql.getJdbcUrl();
            assertThat(jdbcUrl, containsString("?"));
            assertThat(jdbcUrl, containsString("&"));
            assertThat(jdbcUrl, containsString("rewriteBatchedStatements=true"));
            assertThat(jdbcUrl, containsString("allowMultiQueries=true"));
        } finally {
            mysql.stop();
        }
    }
}

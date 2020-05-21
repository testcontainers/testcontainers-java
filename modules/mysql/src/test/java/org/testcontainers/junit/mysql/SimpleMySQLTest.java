package org.testcontainers.junit.mysql;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;


public class SimpleMySQLTest extends AbstractContainerDatabaseTest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleMySQLTest.class);

    /*
     * Ordinarily you wouldn't try and run multiple containers simultaneously - this is just used for testing.
     * To avoid memory issues with the default, low memory, docker machine setup, we instantiate only one container
     * at a time, inside the test methods themselves.
     */
    /*
    @ClassRule
    public static MySQLContainer<?> mysql = new MySQLContainer<>();

    @ClassRule
    public static MySQLContainer<?> mysqlOldVersion = new MySQLContainer<>("mysql:5.5");

    @ClassRule
    public static MySQLContainer<?> mysqlCustomConfig = new MySQLContainer<>("mysql:5.6")
                                                            .withConfigurationOverride("somepath/mysql_conf_override");
    */

    @Test
    public void testSimple() throws SQLException {
        try (MySQLContainer<?> mysql = new MySQLContainer<>()
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
        try (MySQLContainer<?> mysqlOldVersion = new MySQLContainer<>("mysql:5.5")
            .withConfigurationOverride("somepath/mysql_conf_override")
            .withLogConsumer(new Slf4jLogConsumer(logger))) {

            mysqlOldVersion.start();

            ResultSet resultSet = performQuery(mysqlOldVersion, "SELECT VERSION()");
            String resultSetString = resultSet.getString(1);

            assertTrue("The database version can be set using a container rule parameter", resultSetString.startsWith("5.5"));
        }
    }

    @Test
    public void testMySQLWithCustomIniFile() throws SQLException {
        assumeFalse(SystemUtils.IS_OS_WINDOWS);

        try (MySQLContainer<?> mysqlCustomConfig = new MySQLContainer<>("mysql:5.6")
            .withConfigurationOverride("somepath/mysql_conf_override")) {

            mysqlCustomConfig.start();

            ResultSet resultSet = performQuery(mysqlCustomConfig, "SELECT @@GLOBAL.innodb_file_format");
            String result = resultSet.getString(1);

            assertEquals("The InnoDB file format has been set by the ini file content", "Barracuda", result);
        }
    }

    @Test
    public void testCommandOverride() throws SQLException {
        try (MySQLContainer<?> mysqlCustomConfig = new MySQLContainer<>()
            .withCommand("mysqld --auto_increment_increment=42")) {

            mysqlCustomConfig.start();

            ResultSet resultSet = performQuery(mysqlCustomConfig, "show variables like 'auto_increment_increment'");
            String result = resultSet.getString("Value");

            assertEquals("Auto increment increment should be overriden by command line", "42", result);
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (MySQLContainer<?> container = new MySQLContainer<>()
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
        try (MySQLContainer<?> container = new MySQLContainer<>("mysql:5.5")
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
        try (MySQLContainer<?> mysql = new MySQLContainer<>("mysql:5.5")
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
}

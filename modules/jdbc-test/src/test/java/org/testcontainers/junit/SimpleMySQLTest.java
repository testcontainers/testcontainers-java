package org.testcontainers.junit;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;


/**
 * @author richardnorth
 */
public class SimpleMySQLTest extends AbstractContainerDatabaseTest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleMySQLTest.class);

    /*
     * Ordinarily you wouldn't try and run multiple containers simultaneously - this is just used for testing.
     * To avoid memory issues with the default, low memory, docker machine setup, we instantiate only one container
     * at a time, inside the test methods themselves.
     */
    /*
    @ClassRule
    public static MySQLContainer mysql = new MySQLContainer();

    @ClassRule
    public static MySQLContainer mysqlOldVersion = new MySQLContainer("mysql:5.5");

    @ClassRule
    public static MySQLContainer mysqlCustomConfig = new MySQLContainer("mysql:5.6")
                                                            .withConfigurationOverride("somepath/mysql_conf_override");
    */

    @Test
    public void testSimple() throws SQLException {
        MySQLContainer mysql = (MySQLContainer) new MySQLContainer()
                .withConfigurationOverride("somepath/mysql_conf_override")
                .withLogConsumer(new Slf4jLogConsumer(logger));
        mysql.start();

        try {
            ResultSet resultSet = performQuery(mysql, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);

            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        } finally {
            mysql.stop();
        }
    }

    @Test
    public void testSpecificVersion() throws SQLException {
        MySQLContainer mysqlOldVersion = (MySQLContainer) new MySQLContainer("mysql:5.5")
                .withConfigurationOverride("somepath/mysql_conf_override")
                .withLogConsumer(new Slf4jLogConsumer(logger));
        mysqlOldVersion.start();

        try {
            ResultSet resultSet = performQuery(mysqlOldVersion, "SELECT VERSION()");
            String resultSetString = resultSet.getString(1);

            assertTrue("The database version can be set using a container rule parameter", resultSetString.startsWith("5.5"));
        } finally {
            mysqlOldVersion.stop();
        }
    }

    @Test
    public void testMySQLWithCustomIniFile() throws SQLException {
    	assumeFalse(SystemUtils.IS_OS_WINDOWS);
        MySQLContainer mysqlCustomConfig = new MySQLContainer("mysql:5.6")
                                                .withConfigurationOverride("somepath/mysql_conf_override");
        mysqlCustomConfig.start();

        try {
            ResultSet resultSet = performQuery(mysqlCustomConfig, "SELECT @@GLOBAL.innodb_file_format");
            String result = resultSet.getString(1);

            assertEquals("The InnoDB file format has been set by the ini file content", "Barracuda", result);
        } finally {
            mysqlCustomConfig.stop();
        }
    }

    @Test
    public void testCommandOverride() throws SQLException {
        MySQLContainer mysqlCustomConfig = (MySQLContainer) new MySQLContainer().withCommand("mysqld --auto_increment_increment=42");
        mysqlCustomConfig.start();

        try {
            ResultSet resultSet = performQuery(mysqlCustomConfig, "show variables like 'auto_increment_increment'");
            String result = resultSet.getString("Value");

            assertEquals("Auto increment increment should be overriden by command line", "42", result);
        } finally {
            mysqlCustomConfig.stop();
        }

    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (MySQLContainer container = (MySQLContainer) new MySQLContainer()
            .withInitScript("somepath/init_mysql.sql")
            .withLogConsumer(new Slf4jLogConsumer(logger))) {
            container.start();

            ResultSet resultSet = performQuery(container, "SELECT foo FROM bar");
            String firstColumnValue = resultSet.getString(1);

            assertEquals("Value from init script should equal real value", "hello world", firstColumnValue);
        }
     }

    @Test
    public void testEmptyPasswordWithNonRootUser() {

        MySQLContainer container = (MySQLContainer) new MySQLContainer("mysql:5.5").withDatabaseName("TEST")
                .withUsername("test").withPassword("").withEnv("MYSQL_ROOT_HOST", "%");

        try {
            container.start();
            fail("ContainerLaunchException expected to be thrown");
        } catch (ContainerLaunchException e) {
        } finally {
            container.stop();
        }
    }
}

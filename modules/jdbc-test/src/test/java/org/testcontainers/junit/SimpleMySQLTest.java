package org.testcontainers.junit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.NonNull;
import org.apache.commons.lang.SystemUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assume.assumeFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;


/**
 * @author richardnorth
 */
public class SimpleMySQLTest {

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
    public void testMySQL8() throws SQLException {
        assumeFalse(SystemUtils.IS_OS_WINDOWS);
        MySQLContainer container = new MySQLContainer<>("mysql:8.0.11")
            .withCommand("mysqld --default-authentication-plugin=mysql_native_password");
        container.start();

        try {
            ResultSet resultSet = performQuery(container, "SELECT VERSION()");
            String resultSetString = resultSet.getString(1);

            assertTrue("The database version can be set using a container rule parameter", "8.0.11".equals(resultSetString));
        }
        finally {
            container.stop();
        }
    }

    @NonNull
    protected ResultSet performQuery(MySQLContainer containerRule, String sql) throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(containerRule.getDriverClassName());
        hikariConfig.setJdbcUrl(containerRule.getJdbcUrl());
        hikariConfig.setUsername(containerRule.getUsername());
        hikariConfig.setPassword(containerRule.getPassword());

        HikariDataSource ds = new HikariDataSource(hikariConfig);
        Statement statement = ds.getConnection().createStatement();
        statement.execute(sql);
        ResultSet resultSet = statement.getResultSet();

        resultSet.next();
        return resultSet;
    }
}

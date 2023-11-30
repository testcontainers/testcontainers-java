package org.testcontainers.junit.mysql;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.MySQLTestImages;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.io.File;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.Assumptions.assumeThat;

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
        try (
            MySQLContainer<?> mysql = new MySQLContainer<>(MySQLTestImages.MYSQL_57_IMAGE)
                .withLogConsumer(new Slf4jLogConsumer(logger))
        ) {
            mysql.start();

            ResultSet resultSet = performQuery(mysql, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);

            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(mysql);
        }
    }

    @Test
    public void testSpecificVersion() throws SQLException {
        try (
            MySQLContainer<?> mysqlOldVersion = new MySQLContainer<>(MySQLTestImages.MYSQL_56_IMAGE)
                .withConfigurationOverride("somepath/mysql_conf_override")
                .withLogConsumer(new Slf4jLogConsumer(logger))
        ) {
            mysqlOldVersion.start();

            ResultSet resultSet = performQuery(mysqlOldVersion, "SELECT VERSION()");
            String resultSetString = resultSet.getString(1);

            assertThat(resultSetString)
                .as("The database version can be set using a container rule parameter")
                .startsWith("5.6");
        }
    }

    @Test
    public void testMySQLWithCustomIniFile() throws SQLException {
        try (
            MySQLContainer<?> mysqlCustomConfig = new MySQLContainer<>(MySQLTestImages.MYSQL_56_IMAGE)
                .withConfigurationOverride("somepath/mysql_conf_override")
        ) {
            mysqlCustomConfig.start();

            assertThatCustomIniFileWasUsed(mysqlCustomConfig);
        }
    }

    @Test
    public void testCommandOverride() throws SQLException {
        try (
            MySQLContainer<?> mysqlCustomConfig = new MySQLContainer<>(MySQLTestImages.MYSQL_57_IMAGE)
                .withCommand("mysqld --auto_increment_increment=42")
        ) {
            mysqlCustomConfig.start();

            ResultSet resultSet = performQuery(mysqlCustomConfig, "show variables like 'auto_increment_increment'");
            String result = resultSet.getString("Value");

            assertThat(result).as("Auto increment increment should be overriden by command line").isEqualTo("42");
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (
            MySQLContainer<?> container = new MySQLContainer<>(MySQLTestImages.MYSQL_57_IMAGE)
                .withInitScript("somepath/init_mysql.sql")
                .withLogConsumer(new Slf4jLogConsumer(logger))
        ) {
            container.start();

            ResultSet resultSet = performQuery(container, "SELECT foo FROM bar");
            String firstColumnValue = resultSet.getString(1);

            assertThat(firstColumnValue).as("Value from init script should equal real value").isEqualTo("hello world");
        }
    }

    @Test(expected = ContainerLaunchException.class)
    public void testEmptyPasswordWithNonRootUser() {
        try (
            MySQLContainer<?> container = new MySQLContainer<>(MySQLTestImages.MYSQL_56_IMAGE)
                .withDatabaseName("TEST")
                .withUsername("test")
                .withPassword("")
                .withEnv("MYSQL_ROOT_HOST", "%")
        ) {
            container.start();
            fail("ContainerLaunchException expected to be thrown");
        }
    }

    @Test
    public void testEmptyPasswordWithRootUser() throws SQLException {
        // Add MYSQL_ROOT_HOST environment so that we can root login from anywhere for testing purposes
        try (
            MySQLContainer<?> mysql = new MySQLContainer<>(MySQLTestImages.MYSQL_56_IMAGE)
                .withDatabaseName("foo")
                .withUsername("root")
                .withPassword("")
                .withEnv("MYSQL_ROOT_HOST", "%")
        ) {
            mysql.start();

            ResultSet resultSet = performQuery(mysql, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);

            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }

    @Test
    public void testWithAdditionalUrlParamTimeZone() throws SQLException {
        MySQLContainer<?> mysql = new MySQLContainer<>(MySQLTestImages.MYSQL_57_IMAGE)
            .withUrlParam("serverTimezone", "Europe/Zurich")
            .withEnv("TZ", "Europe/Zurich")
            .withLogConsumer(new Slf4jLogConsumer(logger));
        mysql.start();

        try (Connection connection = mysql.createConnection("")) {
            Statement statement = connection.createStatement();
            statement.execute("SELECT NOW();");
            try (ResultSet resultSet = statement.getResultSet()) {
                resultSet.next();

                // checking that the time_zone MySQL is Europe/Zurich
                LocalDateTime localDateTime = resultSet.getObject(1, LocalDateTime.class);
                ZonedDateTime actualDateTime = localDateTime
                    .atZone(ZoneId.of("Europe/Zurich"))
                    .truncatedTo(ChronoUnit.MINUTES);
                ZonedDateTime expectedDateTime = ZonedDateTime
                    .now(ZoneId.of("Europe/Zurich"))
                    .truncatedTo(ChronoUnit.MINUTES);

                String message = String.format(
                    "MySQL time zone is not Europe/Zurich. MySQL date:%s, current date:%s",
                    actualDateTime,
                    expectedDateTime
                );
                assertThat(actualDateTime).as(message).isEqualTo(expectedDateTime);
            }
        } finally {
            mysql.stop();
        }
    }

    @Test
    public void testWithAdditionalUrlParamMultiQueries() throws SQLException {
        MySQLContainer<?> mysql = new MySQLContainer<>(MySQLTestImages.MYSQL_57_IMAGE)
            .withUrlParam("allowMultiQueries", "true")
            .withLogConsumer(new Slf4jLogConsumer(logger));
        mysql.start();

        try (Connection connection = mysql.createConnection("")) {
            Statement statement = connection.createStatement();
            String multiQuery =
                "DROP TABLE IF EXISTS bar; " +
                "CREATE TABLE bar (foo VARCHAR(20)); " +
                "INSERT INTO bar (foo) VALUES ('hello world');";
            statement.execute(multiQuery);
            statement.execute("SELECT foo FROM bar;");
            try (ResultSet resultSet = statement.getResultSet()) {
                resultSet.next();
                String firstColumnValue = resultSet.getString(1);
                assertThat(firstColumnValue).as("Value from bar should equal real value").isEqualTo("hello world");
            }
        } finally {
            mysql.stop();
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        MySQLContainer<?> mysql = new MySQLContainer<>(MySQLTestImages.MYSQL_57_IMAGE)
            .withUrlParam("allowMultiQueries", "true")
            .withUrlParam("rewriteBatchedStatements", "true")
            .withLogConsumer(new Slf4jLogConsumer(logger));

        try {
            mysql.start();
            String jdbcUrl = mysql.getJdbcUrl();
            assertThat(jdbcUrl).contains("?");
            assertThat(jdbcUrl).contains("&");
            assertThat(jdbcUrl).contains("rewriteBatchedStatements=true");
            assertThat(jdbcUrl).contains("allowMultiQueries=true");
        } finally {
            mysql.stop();
        }
    }

    @Test
    public void testWithOnlyUserReadableCustomIniFile() throws Exception {
        assumeThat(FileSystems.getDefault().supportedFileAttributeViews().contains("posix")).isTrue();
        try (
            MySQLContainer<?> mysql = new MySQLContainer<>(MySQLTestImages.MYSQL_56_IMAGE)
                .withConfigurationOverride("somepath/mysql_conf_override")
                .withLogConsumer(new Slf4jLogConsumer(logger))
        ) {
            URL resource = this.getClass().getClassLoader().getResource("somepath/mysql_conf_override");

            File file = new File(resource.toURI());
            assertThat(file.isDirectory()).isTrue();

            Set<PosixFilePermission> permissions = new HashSet<>(
                Arrays.asList(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
                )
            );

            Files.setPosixFilePermissions(file.toPath(), permissions);

            mysql.start();
            assertThatCustomIniFileWasUsed(mysql);
        }
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(MySQLContainer<?> mysql) {
        assertThat(mysql.getExposedPorts()).containsExactly(MySQLContainer.MYSQL_PORT);
        assertThat(mysql.getLivenessCheckPortNumbers()).containsExactly(mysql.getMappedPort(MySQLContainer.MYSQL_PORT));
    }

    private void assertThatCustomIniFileWasUsed(MySQLContainer<?> mysql) throws SQLException {
        try (ResultSet resultSet = performQuery(mysql, "SELECT @@GLOBAL.innodb_file_format")) {
            String result = resultSet.getString(1);
            assertThat(result).as("The InnoDB file format has been set by the ini file content").isEqualTo("Barracuda");
        }
    }
}

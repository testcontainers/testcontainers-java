package org.testcontainers.mariadb;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;
import org.testcontainers.MariaDBTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.io.File;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

class MariaDBContainerTest extends AbstractContainerDatabaseTest {

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            MariaDBContainer mariadb = new MariaDBContainer("mariadb:10.3.39")
            // }
        ) {
            mariadb.start();

            ResultSet resultSet = performQuery(mariadb, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);

            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }

    @Test
    void testSpecificVersion() throws SQLException {
        try (
            MariaDBContainer mariadbOldVersion = new MariaDBContainer(
                MariaDBTestImages.MARIADB_IMAGE.withTag("10.3.39")
            )
        ) {
            mariadbOldVersion.start();

            ResultSet resultSet = performQuery(mariadbOldVersion, "SELECT VERSION()");
            String resultSetString = resultSet.getString(1);

            assertThat(resultSetString)
                .as("The database version can be set using a container rule parameter")
                .startsWith("10.3.39");
        }
    }

    @Test
    void testMariaDBWithCustomIniFile() throws SQLException {
        assumeThat(SystemUtils.IS_OS_WINDOWS).isFalse();

        try (
            MariaDBContainer mariadbCustomConfig = new MariaDBContainer(
                MariaDBTestImages.MARIADB_IMAGE.withTag("10.3.39")
            )
                .withConfigurationOverride("somepath/mariadb_conf_override")
        ) {
            mariadbCustomConfig.start();

            assertThatCustomIniFileWasUsed(mariadbCustomConfig);
        }
    }

    @Test
    void testMariaDBWithCommandOverride() throws SQLException {
        try (
            MariaDBContainer mariadbCustomConfig = new MariaDBContainer(MariaDBTestImages.MARIADB_IMAGE)
                .withCommand("mysqld --auto_increment_increment=10")
        ) {
            mariadbCustomConfig.start();
            ResultSet resultSet = performQuery(mariadbCustomConfig, "show variables like 'auto_increment_increment'");
            String result = resultSet.getString("Value");

            assertThat(result).as("Auto increment increment should be overridden by command line").isEqualTo("10");
        }
    }

    @Test
    void testWithAdditionalUrlParamInJdbcUrl() {
        MariaDBContainer mariaDBContainer = new MariaDBContainer(MariaDBTestImages.MARIADB_IMAGE)
            .withUrlParam("connectTimeout", "40000")
            .withUrlParam("rewriteBatchedStatements", "true");

        try {
            mariaDBContainer.start();
            String jdbcUrl = mariaDBContainer.getJdbcUrl();
            assertThat(jdbcUrl).contains("?");
            assertThat(jdbcUrl).contains("&");
            assertThat(jdbcUrl).contains("rewriteBatchedStatements=true");
            assertThat(jdbcUrl).contains("connectTimeout=40000");
        } finally {
            mariaDBContainer.stop();
        }
    }

    @Test
    void testWithOnlyUserReadableCustomIniFile() throws Exception {
        assumeThat(FileSystems.getDefault().supportedFileAttributeViews().contains("posix")).isTrue();

        try (
            MariaDBContainer mariadbCustomConfig = new MariaDBContainer(
                MariaDBTestImages.MARIADB_IMAGE.withTag("10.3.39")
            )
                .withConfigurationOverride("somepath/mariadb_conf_override")
        ) {
            URL resource = this.getClass().getClassLoader().getResource("somepath/mariadb_conf_override");

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

            mariadbCustomConfig.start();

            assertThatCustomIniFileWasUsed(mariadbCustomConfig);
        }
    }

    @Test
    void testEmptyPasswordWithRootUser() throws SQLException {
        try (MariaDBContainer mysql = new MariaDBContainer("mariadb:11.2.4").withUsername("root")) {
            mysql.start();

            ResultSet resultSet = performQuery(mysql, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);

            assertThat(resultSetInt).isEqualTo(1);
        }
    }

    private void assertThatCustomIniFileWasUsed(MariaDBContainer mariadb) throws SQLException {
        try (ResultSet resultSet = performQuery(mariadb, "SELECT @@GLOBAL.innodb_max_undo_log_size")) {
            long result = resultSet.getLong(1);
            assertThat(result)
                .as("The InnoDB max undo log size has been set by the ini file content")
                .isEqualTo(20000000);
        }
    }
}

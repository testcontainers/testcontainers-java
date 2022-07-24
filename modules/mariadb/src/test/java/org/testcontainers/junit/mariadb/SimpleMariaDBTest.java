package org.testcontainers.junit.mariadb;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.testcontainers.MariaDBTestImages;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.SQLException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

public class SimpleMariaDBTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (MariaDBContainer<?> mariadb = new MariaDBContainer<>(MariaDBTestImages.MARIADB_IMAGE)) {
            mariadb.start();

            assertQuery(
                mariadb,
                "SELECT 1",
                rs -> {
                    int resultSetInt = rs.getInt(1);
                    assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
                }
            );
        }
    }

    @Test
    public void testSpecificVersion() throws SQLException {
        try (
            MariaDBContainer<?> mariadbOldVersion = new MariaDBContainer<>(
                MariaDBTestImages.MARIADB_IMAGE.withTag("5.5.51")
            )
        ) {
            mariadbOldVersion.start();
            assertQuery(
                mariadbOldVersion,
                "SELECT VERSION()",
                rs -> {
                    String resultSetString = rs.getString(1);
                    assertTrue(
                        "The database version can be set using a container rule parameter",
                        resultSetString.startsWith("5.5.51")
                    );
                }
            );
        }
    }

    @Test
    public void testMariaDBWithCustomIniFile() throws SQLException {
        assumeFalse(SystemUtils.IS_OS_WINDOWS);

        try (
            MariaDBContainer<?> mariadbCustomConfig = new MariaDBContainer<>(
                MariaDBTestImages.MARIADB_IMAGE.withTag("10.1.16")
            )
                .withConfigurationOverride("somepath/mariadb_conf_override")
        ) {
            mariadbCustomConfig.start();

            assertQuery(
                mariadbCustomConfig,
                "SELECT @@GLOBAL.innodb_file_format",
                rs -> {
                    String result = rs.getString(1);
                    assertEquals("The InnoDB file format has been set by the ini file content", "Barracuda", result);
                }
            );
        }
    }

    @Test
    public void testMariaDBWithCommandOverride() throws SQLException {
        try (
            MariaDBContainer<?> mariadbCustomConfig = new MariaDBContainer<>(MariaDBTestImages.MARIADB_IMAGE)
                .withCommand("mysqld --auto_increment_increment=10")
        ) {
            mariadbCustomConfig.start();
            assertQuery(
                mariadbCustomConfig,
                "show variables like 'auto_increment_increment'",
                rs -> {
                    String result = rs.getString("Value");
                    assertEquals("Auto increment increment should be overriden by command line", "10", result);
                }
            );
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MariaDBTestImages.MARIADB_IMAGE)
            .withUrlParam("connectTimeout", "40000")
            .withUrlParam("rewriteBatchedStatements", "true");

        try {
            mariaDBContainer.start();
            String jdbcUrl = mariaDBContainer.getJdbcUrl();
            assertThat(jdbcUrl, containsString("?"));
            assertThat(jdbcUrl, containsString("&"));
            assertThat(jdbcUrl, containsString("rewriteBatchedStatements=true"));
            assertThat(jdbcUrl, containsString("connectTimeout=40000"));
        } finally {
            mariaDBContainer.stop();
        }
    }
}

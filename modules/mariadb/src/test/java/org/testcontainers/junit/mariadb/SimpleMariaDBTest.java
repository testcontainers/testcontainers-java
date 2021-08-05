package org.testcontainers.junit.mariadb;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.MariaDBTestImages.MARIADB_IMAGE;


public class SimpleMariaDBTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (MariaDBContainer<?> mariadb = new MariaDBContainer<>(MARIADB_IMAGE)) {

            mariadb.start();

            ResultSet resultSet = performQuery(mariadb, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);

            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testSpecificVersion() throws SQLException {
        try (MariaDBContainer<?> mariadbOldVersion = new MariaDBContainer<>(MARIADB_IMAGE.withTag("5.5.51"))) {

            mariadbOldVersion.start();

            ResultSet resultSet = performQuery(mariadbOldVersion, "SELECT VERSION()");
            String resultSetString = resultSet.getString(1);

            assertTrue("The database version can be set using a container rule parameter", resultSetString.startsWith("5.5.51"));
        }
    }

    @Test
    public void testMariaDBWithCustomIniFile() throws SQLException {
        assumeFalse(SystemUtils.IS_OS_WINDOWS);

        try (MariaDBContainer<?> mariadbCustomConfig = new MariaDBContainer<>(MARIADB_IMAGE.withTag("10.1.16"))
            .withConfigurationOverride("somepath/mariadb_conf_override")) {
            mariadbCustomConfig.start();

            ResultSet resultSet = performQuery(mariadbCustomConfig, "SELECT @@GLOBAL.innodb_file_format");
            String result = resultSet.getString(1);

            assertEquals("The InnoDB file format has been set by the ini file content", "Barracuda", result);
        }
    }

    @Test
    public void testMariaDBWithCommandOverride() throws SQLException {

        try (MariaDBContainer<?> mariadbCustomConfig = new MariaDBContainer<>(MARIADB_IMAGE)
            .withCommand("mysqld --auto_increment_increment=10")) {
            mariadbCustomConfig.start();
            ResultSet resultSet = performQuery(mariadbCustomConfig, "show variables like 'auto_increment_increment'");
            String result = resultSet.getString("Value");

            assertEquals("Auto increment increment should be overriden by command line", "10", result);
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>(MARIADB_IMAGE)
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

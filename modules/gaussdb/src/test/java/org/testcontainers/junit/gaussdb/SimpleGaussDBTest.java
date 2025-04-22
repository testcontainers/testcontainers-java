package org.testcontainers.junit.gaussdb;

import org.junit.Test;
import org.testcontainers.GaussDBTestImages;
import org.testcontainers.containers.GaussDBContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

public class SimpleGaussDBTest extends AbstractContainerDatabaseTest {
    static {
        // Gaussdb JDBC driver uses JUL; disable it to avoid annoying, irrelevant, stderr logs during connection testing
        LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
    }

    @Test
    public void testSimple() throws SQLException {
        try (GaussDBContainer<?> gaussdb = new GaussDBContainer<>(GaussDBTestImages.GAUSSDB_TEST_IMAGE)) {
            gaussdb.start();

            ResultSet resultSet = performQuery(gaussdb, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(gaussdb);
        }
    }

    @Test
    public void testCommandOverride() throws SQLException {
        try (
            GaussDBContainer<?> gauss = new GaussDBContainer<>(GaussDBTestImages.GAUSSDB_TEST_IMAGE)
                .withCommand("gaussdb", "-N", "42")
        ) {
            gauss.start();

            ResultSet resultSet = performQuery(gauss, "SELECT current_setting('max_connections')");
            String result = resultSet.getString(1);
            assertThat(result).as("max_connections should be overridden").isEqualTo("42");
        }
    }

    @Test
    public void testUnsetCommand() throws SQLException {
        try (
            GaussDBContainer<?> gaussdb = new GaussDBContainer<>(GaussDBTestImages.GAUSSDB_TEST_IMAGE)
                .withCommand("gaussdb", "-N", "42")
                .withCommand()
        ) {
            gaussdb.start();

            ResultSet resultSet = performQuery(gaussdb, "SELECT current_setting('max_connections')");
            String result = resultSet.getString(1);
            assertThat(result).as("max_connections should not be overridden").isNotEqualTo("42");
        }
    }

    @Test
    public void testMissingInitScript() {
        try (
            GaussDBContainer<?> gaussdb = new GaussDBContainer<>(GaussDBTestImages.GAUSSDB_TEST_IMAGE)
                .withInitScript(null)
        ) {
            assertThatNoException().isThrownBy(gaussdb::start);
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (
            GaussDBContainer<?> gaussdb = new GaussDBContainer<>(GaussDBTestImages.GAUSSDB_TEST_IMAGE)
                .withInitScript("somepath/init_gaussdb.sql")
        ) {
            gaussdb.start();

            ResultSet resultSet = performQuery(gaussdb, "SELECT foo FROM bar");

            String firstColumnValue = resultSet.getString(1);
            assertThat(firstColumnValue).as("Value from init script should equal real value").isEqualTo("hello world");
        }
    }

    @Test
    public void testExplicitInitScripts() throws SQLException {
        try (
            GaussDBContainer<?> gaussdb = new GaussDBContainer<>(GaussDBTestImages.GAUSSDB_TEST_IMAGE)
                .withInitScripts("somepath/init_gaussdb.sql", "somepath/init_gaussdb_2.sql")
        ) {
            gaussdb.start();

            ResultSet resultSet = performQuery(
                gaussdb,
                "SELECT foo AS value FROM bar UNION SELECT bar AS value FROM foo"
            );

            String columnValue1 = resultSet.getString(1);
            resultSet.next();
            String columnValue2 = resultSet.getString(1);
            assertThat(columnValue1).as("Value from init script 1 should equal real value").isEqualTo("hello world");
            assertThat(columnValue2).as("Value from init script 2 should equal real value").isEqualTo("hello world 2");
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        try (
            GaussDBContainer<?> gaussdb = new GaussDBContainer<>(GaussDBTestImages.GAUSSDB_TEST_IMAGE)
                .withUrlParam("charSet", "UNICODE")
        ) {
            gaussdb.start();
            String jdbcUrl = gaussdb.getJdbcUrl();
            assertThat(jdbcUrl).contains("?");
            assertThat(jdbcUrl).contains("&");
            assertThat(jdbcUrl).contains("charSet=UNICODE");
        }
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(GaussDBContainer<?> gaussdb) {
        assertThat(gaussdb.getExposedPorts()).containsExactly(GaussDBContainer.GaussDB_PORT);
        assertThat(gaussdb.getLivenessCheckPortNumbers())
            .containsExactly(gaussdb.getMappedPort(GaussDBContainer.GaussDB_PORT));
    }
}

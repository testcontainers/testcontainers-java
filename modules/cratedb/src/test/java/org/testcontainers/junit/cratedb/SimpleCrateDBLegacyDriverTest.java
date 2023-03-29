package org.testcontainers.junit.cratedb;

import org.junit.Test;
import org.testcontainers.CrateDBTestImages;
import org.testcontainers.containers.CrateDBContainer;
import org.testcontainers.containers.CrateDBLegacyDriverContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleCrateDBLegacyDriverTest extends AbstractContainerDatabaseTest {
    static {
        // Postgres JDBC driver uses JUL; disable it to avoid annoying, irrelevant, stderr logs during connection testing
        LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
    }

    @Test
    public void testSimple() throws SQLException {
        try (
            CrateDBLegacyDriverContainer cratedb = new CrateDBLegacyDriverContainer(
                CrateDBTestImages.CRATEDB_TEST_IMAGE
            )
        ) {
            cratedb.start();

            ResultSet resultSet = performQuery(cratedb, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(cratedb);
        }
    }

    @Test
    public void testCommandOverride() throws SQLException {
        try (
            CrateDBContainer cratedb = new CrateDBLegacyDriverContainer(CrateDBTestImages.CRATEDB_TEST_IMAGE)
                .withCommand("crate -C cluster.name=testcontainers")
        ) {
            cratedb.start();

            ResultSet resultSet = performQuery(cratedb, "select name from sys.cluster");
            String result = resultSet.getString(1);
            assertThat(result).as("cluster name should be overriden").isEqualTo("testcontainers");
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (
            CrateDBContainer cratedb = new CrateDBLegacyDriverContainer(CrateDBTestImages.CRATEDB_TEST_IMAGE)
                .withInitScript("somepath/init_cratedb.sql")
        ) {
            cratedb.start();

            ResultSet resultSet = performQuery(cratedb, "SELECT foo FROM bar");

            String firstColumnValue = resultSet.getString(1);
            assertThat(firstColumnValue).as("Value from init script should equal real value").isEqualTo("hello world");
        }
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(CrateDBLegacyDriverContainer cratedb) {
        assertThat(cratedb.getExposedPorts())
            .containsExactly(CrateDBContainer.CRATEDB_PG_PORT, CrateDBContainer.CRATEDB_HTTP_PORT);
        assertThat(cratedb.getLivenessCheckPortNumbers())
            .containsExactlyInAnyOrder(
                cratedb.getMappedPort(CrateDBContainer.CRATEDB_PG_PORT),
                cratedb.getMappedPort(CrateDBContainer.CRATEDB_HTTP_PORT)
            );
    }
}

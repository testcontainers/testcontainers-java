package org.testcontainers.junit.cratedb;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.CrateDBTestImages;
import org.testcontainers.cratedb.CrateDBContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleCrateDBTest extends AbstractContainerDatabaseTest {
    static {
        // Postgres JDBC driver uses JUL; disable it to avoid annoying, irrelevant, stderr logs during connection testing
        LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
    }

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            CrateDBContainer cratedb = new CrateDBContainer("crate:5.2.5")
            // }
        ) {
            cratedb.start();

            executeSelectOneQuery(cratedb);
            assertHasCorrectExposedAndLivenessCheckPorts(cratedb);
        }
    }

    @Test
    void testCommandOverride() throws SQLException {
        try (
            CrateDBContainer cratedb = new CrateDBContainer(CrateDBTestImages.CRATEDB_TEST_IMAGE)
                .withCommand("crate -C discovery.type=single-node -C cluster.name=testcontainers")
        ) {
            cratedb.start();

            executeQuery(
                cratedb,
                "select name from sys.cluster",
                resultSet -> {
                    Assertions
                        .assertThatNoException()
                        .isThrownBy(() -> {
                            String result = resultSet.getString(1);
                            assertThat(result).as("cluster name should be overridden").isEqualTo("testcontainers");
                        });
                }
            );
        }
    }

    @Test
    void testExplicitInitScript() throws SQLException {
        try (
            CrateDBContainer cratedb = new CrateDBContainer(CrateDBTestImages.CRATEDB_TEST_IMAGE)
                .withInitScript("somepath/init_cratedb.sql")
        ) {
            cratedb.start();

            executeSelectFooBarQuery(cratedb);
        }
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(CrateDBContainer cratedb) {
        assertThat(cratedb.getExposedPorts()).containsExactlyInAnyOrder(5432, 4200);
        assertThat(cratedb.getLivenessCheckPortNumbers())
            .containsExactlyInAnyOrder(cratedb.getMappedPort(5432), cratedb.getMappedPort(4200));
    }
}

package org.testcontainers.junit.cratedb;

import org.junit.jupiter.api.Test;
import org.testcontainers.CrateDBTestImages;
import org.testcontainers.cratedb.CrateDBContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

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

            performQuery(
                cratedb,
                "SELECT 1",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            int resultSetInt = resultSet.getInt(1);
                            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
                            assertHasCorrectExposedAndLivenessCheckPorts(cratedb);
                        });
                }
            );
        }
    }

    @Test
    void testCommandOverride() throws SQLException {
        try (
            CrateDBContainer cratedb = new CrateDBContainer(CrateDBTestImages.CRATEDB_TEST_IMAGE)
                .withCommand("crate -C discovery.type=single-node -C cluster.name=testcontainers")
        ) {
            cratedb.start();

            performQuery(
                cratedb,
                "select name from sys.cluster",
                resultSet -> {
                    assertThatNoException()
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

            performQuery(
                cratedb,
                "SELECT foo FROM bar",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            String firstColumnValue = resultSet.getString(1);
                            assertThat(firstColumnValue)
                                .as("Value from init script should equal real value")
                                .isEqualTo("hello world");
                        });
                }
            );
        }
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(CrateDBContainer cratedb) {
        assertThat(cratedb.getExposedPorts()).containsExactlyInAnyOrder(5432, 4200);
        assertThat(cratedb.getLivenessCheckPortNumbers())
            .containsExactlyInAnyOrder(cratedb.getMappedPort(5432), cratedb.getMappedPort(4200));
    }
}

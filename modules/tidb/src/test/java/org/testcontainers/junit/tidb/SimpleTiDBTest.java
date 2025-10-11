package org.testcontainers.junit.tidb;

import org.junit.jupiter.api.Test;
import org.testcontainers.TiDBTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.tidb.TiDBContainer;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class SimpleTiDBTest extends AbstractContainerDatabaseTest {

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            TiDBContainer tidb = new TiDBContainer("pingcap/tidb:v6.1.0")
            // }
        ) {
            tidb.start();

            performQuery(
                tidb,
                "SELECT 1",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            int resultSetInt = resultSet.getInt(1);
                            assertThat(resultSetInt).isEqualTo(1);
                            assertHasCorrectExposedAndLivenessCheckPorts(tidb);
                        });
                }
            );
        }
    }

    @Test
    void testExplicitInitScript() throws SQLException {
        try (
            TiDBContainer tidb = new TiDBContainer(TiDBTestImages.TIDB_IMAGE).withInitScript("somepath/init_tidb.sql")
        ) { // TiDB is expected to be compatible with MySQL
            tidb.start();

            performQuery(
                tidb,
                "SELECT foo FROM bar",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            String firstColumnValue = resultSet.getString(1);
                            assertThat(firstColumnValue).isEqualTo("hello world");
                        });
                }
            );
        }
    }

    @Test
    void testWithAdditionalUrlParamInJdbcUrl() {
        try (TiDBContainer tidb = new TiDBContainer(TiDBTestImages.TIDB_IMAGE).withUrlParam("sslmode", "disable")) {
            tidb.start();
            String jdbcUrl = tidb.getJdbcUrl();
            assertThat(jdbcUrl).contains("?");
            assertThat(jdbcUrl).contains("sslmode=disable");
        }
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(TiDBContainer tidb) {
        int tidbPort = 4000;
        int restApiPort = 10080;

        assertThat(tidb.getExposedPorts()).containsExactlyInAnyOrder(tidbPort, restApiPort);
        assertThat(tidb.getLivenessCheckPortNumbers())
            .containsExactlyInAnyOrder(tidb.getMappedPort(tidbPort), tidb.getMappedPort(restApiPort));
    }
}

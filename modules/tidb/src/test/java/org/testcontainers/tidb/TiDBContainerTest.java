package org.testcontainers.tidb;

import org.junit.jupiter.api.Test;
import org.testcontainers.TiDBTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class TiDBContainerTest extends AbstractContainerDatabaseTest {

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            TiDBContainer tidb = new TiDBContainer("pingcap/tidb:v6.1.0")
            // }
        ) {
            tidb.start();

            ResultSet resultSet = performQuery(tidb, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(tidb);
        }
    }

    @Test
    void testExplicitInitScript() throws SQLException {
        try (
            TiDBContainer tidb = new TiDBContainer(TiDBTestImages.TIDB_IMAGE).withInitScript("somepath/init_tidb.sql")
        ) { // TiDB is expected to be compatible with MySQL
            tidb.start();

            ResultSet resultSet = performQuery(tidb, "SELECT foo FROM bar");

            String firstColumnValue = resultSet.getString(1);
            assertThat(firstColumnValue).isEqualTo("hello world");
        }
    }

    @Test
    void testWithAdditionalUrlParamInJdbcUrl() {
        TiDBContainer tidb = new TiDBContainer(TiDBTestImages.TIDB_IMAGE).withUrlParam("sslmode", "disable");

        try {
            tidb.start();
            String jdbcUrl = tidb.getJdbcUrl();
            assertThat(jdbcUrl).contains("?");
            assertThat(jdbcUrl).contains("sslmode=disable");
        } finally {
            tidb.stop();
        }
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(TiDBContainer tidb) {
        Integer tidbPort = 4000;
        Integer restApiPort = 10080;

        assertThat(tidb.getExposedPorts()).containsExactlyInAnyOrder(tidbPort, restApiPort);
        assertThat(tidb.getLivenessCheckPortNumbers())
            .containsExactlyInAnyOrder(tidb.getMappedPort(tidbPort), tidb.getMappedPort(restApiPort));
    }
}

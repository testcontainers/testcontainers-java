package org.testcontainers.junit.tidb;

import org.junit.jupiter.api.Test;
import org.testcontainers.TiDBTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.tidb.TiDBContainer;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleTiDBTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (TiDBContainer tidb = new TiDBContainer(TiDBTestImages.TIDB_IMAGE)) {
            tidb.start();

            ResultSet resultSet = performQuery(tidb, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(tidb);
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
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
    public void testWithAdditionalUrlParamInJdbcUrl() {
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

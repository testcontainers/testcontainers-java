package org.testcontainers.oceanbase;

import org.junit.jupiter.api.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleOceanBaseCETest extends AbstractContainerDatabaseTest {

    private static final String IMAGE = "oceanbase/oceanbase-ce:4.2.1.8-108000022024072217";

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            OceanBaseCEContainer oceanbase = new OceanBaseCEContainer(
                "oceanbase/oceanbase-ce:4.2.1.8-108000022024072217"
            )
            // }
        ) {
            oceanbase.start();

            ResultSet resultSet = performQuery(oceanbase, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(oceanbase);
        }
    }

    @Test
    void testExplicitInitScript() throws SQLException {
        try (OceanBaseCEContainer oceanbase = new OceanBaseCEContainer(IMAGE).withInitScript("init.sql")) {
            oceanbase.start();

            ResultSet resultSet = performQuery(oceanbase, "SELECT foo FROM bar");
            String firstColumnValue = resultSet.getString(1);
            assertThat(firstColumnValue).as("Value from init script should equal real value").isEqualTo("hello world");
        }
    }

    @Test
    void testWithAdditionalUrlParamInJdbcUrl() {
        try (OceanBaseCEContainer oceanbase = new OceanBaseCEContainer(IMAGE).withUrlParam("useSSL", "false")) {
            oceanbase.start();

            String jdbcUrl = oceanbase.getJdbcUrl();
            assertThat(jdbcUrl).contains("?");
            assertThat(jdbcUrl).contains("useSSL=false");
        }
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(OceanBaseCEContainer oceanbase) {
        int sqlPort = 2881;
        int rpcPort = 2882;

        assertThat(oceanbase.getExposedPorts()).containsExactlyInAnyOrder(sqlPort, rpcPort);
        assertThat(oceanbase.getLivenessCheckPortNumbers())
            .containsExactlyInAnyOrder(oceanbase.getMappedPort(sqlPort), oceanbase.getMappedPort(rpcPort));
    }
}

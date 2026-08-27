package org.testcontainers.oceanbase;

import org.junit.jupiter.api.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;

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

            executeSelectOneQuery(oceanbase);

            assertHasCorrectExposedAndLivenessCheckPorts(oceanbase);
        }
    }

    @Test
    void testExplicitInitScript() throws SQLException {
        try (OceanBaseCEContainer oceanbase = new OceanBaseCEContainer(IMAGE).withInitScript("init.sql")) {
            oceanbase.start();

            executeSelectFooBarQuery(oceanbase);
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

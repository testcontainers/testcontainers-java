package org.testcontainers.doris;

import org.junit.jupiter.api.Test;
import org.testcontainers.DorisTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class DorisContainerTest extends AbstractContainerDatabaseTest {

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            DorisContainer doris = new DorisContainer("apache/doris:3.1.0")
            // }
        ) {
            doris.start();

            ResultSet resultSet = performQuery(doris, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(doris);
        }
    }

    @Test
    void testExplicitInitScript() throws SQLException {
        try (
            DorisContainer doris = new DorisContainer(DorisTestImages.DORIS_IMAGE)
                .withInitScript("somepath/init_doris.sql")
        ) {
            doris.start();

            ResultSet resultSet = performQuery(doris, "SELECT foo FROM bar");

            String firstColumnValue = resultSet.getString(1);
            assertThat(firstColumnValue).isEqualTo("hello world");
        }
    }

    @Test
    void testWithAdditionalUrlParamInJdbcUrl() {
        DorisContainer doris = new DorisContainer(DorisTestImages.DORIS_IMAGE).withUrlParam("useSSL", "false");

        try {
            doris.start();
            String jdbcUrl = doris.getJdbcUrl();
            assertThat(jdbcUrl).contains("?");
            assertThat(jdbcUrl).contains("useSSL=false");
        } finally {
            doris.stop();
        }
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(DorisContainer doris) {
        Integer queryPort = 9030;
        Integer httpPort = 8030;

        assertThat(doris.getExposedPorts()).containsExactlyInAnyOrder(queryPort, httpPort);
        assertThat(doris.getLivenessCheckPortNumbers())
            .containsExactlyInAnyOrder(doris.getMappedPort(queryPort), doris.getMappedPort(httpPort));
    }
}

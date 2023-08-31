package org.testcontainers.junit.oceanbase;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.OceanBaseTestImages;
import org.testcontainers.containers.OceanBaseContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleOceanBaseTest extends AbstractContainerDatabaseTest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleOceanBaseTest.class);

    @Test
    public void testSimple() throws SQLException {
        try (
            OceanBaseContainer container = new OceanBaseContainer(OceanBaseTestImages.OCEANBASE_CE_IMAGE)
                .withMode("slim")
                .enableFastboot()
                .withLogConsumer(new Slf4jLogConsumer(logger))
        ) {
            container.start();

            ResultSet resultSet = performQuery(container, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(container);
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (
            OceanBaseContainer container = new OceanBaseContainer(OceanBaseTestImages.OCEANBASE_CE_IMAGE)
                .withMode("slim")
                .enableFastboot()
                .withInitScript("init.sql")
                .withLogConsumer(new Slf4jLogConsumer(logger))
        ) {
            container.start();

            ResultSet resultSet = performQuery(container, "SELECT foo FROM bar");
            String firstColumnValue = resultSet.getString(1);
            assertThat(firstColumnValue).as("Value from init script should equal real value").isEqualTo("hello world");
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        try (
            OceanBaseContainer container = new OceanBaseContainer(OceanBaseTestImages.OCEANBASE_CE_IMAGE)
                .withMode("slim")
                .enableFastboot()
                .withUrlParam("useSSL", "false")
                .withLogConsumer(new Slf4jLogConsumer(logger))
        ) {
            container.start();

            String jdbcUrl = container.getJdbcUrl();
            assertThat(jdbcUrl).contains("?");
            assertThat(jdbcUrl).contains("useSSL=false");
        }
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(OceanBaseContainer container) {
        int sqlPort = 2881;
        int rpcPort = 2882;

        assertThat(container.getExposedPorts()).containsExactlyInAnyOrder(sqlPort, rpcPort);
        assertThat(container.getLivenessCheckPortNumbers())
            .containsExactlyInAnyOrder(container.getMappedPort(sqlPort), container.getMappedPort(rpcPort));
    }
}

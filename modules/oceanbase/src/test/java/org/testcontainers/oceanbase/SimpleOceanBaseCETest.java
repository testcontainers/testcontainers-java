package org.testcontainers.oceanbase;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleOceanBaseCETest extends AbstractContainerDatabaseTest {

    private static final Logger logger = LoggerFactory.getLogger(SimpleOceanBaseCETest.class);

    private final OceanBaseCEContainerProvider containerProvider = new OceanBaseCEContainerProvider();

    @SuppressWarnings("resource")
    private OceanBaseCEContainer testContainer() {
        return ((OceanBaseCEContainer) containerProvider.newInstance()).withEnv("MODE", "slim")
            .withEnv("FASTBOOT", "true")
            .withLogConsumer(new Slf4jLogConsumer(logger));
    }

    @Test
    public void testSimple() throws SQLException {
        try (OceanBaseCEContainer container = testContainer()) {
            container.start();

            ResultSet resultSet = performQuery(container, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(container);
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (OceanBaseCEContainer container = testContainer().withInitScript("init.sql")) {
            container.start();

            ResultSet resultSet = performQuery(container, "SELECT foo FROM bar");
            String firstColumnValue = resultSet.getString(1);
            assertThat(firstColumnValue).as("Value from init script should equal real value").isEqualTo("hello world");
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        try (OceanBaseCEContainer container = testContainer().withUrlParam("useSSL", "false")) {
            container.start();

            String jdbcUrl = container.getJdbcUrl();
            assertThat(jdbcUrl).contains("?");
            assertThat(jdbcUrl).contains("useSSL=false");
        }
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(OceanBaseCEContainer container) {
        int sqlPort = 2881;
        int rpcPort = 2882;

        assertThat(container.getExposedPorts()).containsExactlyInAnyOrder(sqlPort, rpcPort);
        assertThat(container.getLivenessCheckPortNumbers())
            .containsExactlyInAnyOrder(container.getMappedPort(sqlPort), container.getMappedPort(rpcPort));
    }
}

package org.testcontainers.clickhouse;

import org.junit.Test;
import org.testcontainers.ClickhouseTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class ClickHouseContainerTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (ClickHouseContainer clickhouse = new ClickHouseContainer("clickhouse/clickhouse-server:21.9.2-alpine")) {
            clickhouse.start();

            ResultSet resultSet = performQuery(clickhouse, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
        }
    }

    @Test
    public void customCredentialsWithUrlParams() throws SQLException {
        try (
            ClickHouseContainer clickhouse = new ClickHouseContainer("clickhouse/clickhouse-server:21.9.2-alpine")
                .withUsername("default")
                .withPassword("")
                .withDatabaseName("test")
                .withUrlParam("max_result_rows", "5")
        ) {
            clickhouse.start();

            ResultSet resultSet = performQuery(
                clickhouse,
                "SELECT value FROM system.settings where name='max_result_rows'"
            );

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(5);
        }
    }

    @Test
    public void testNewAuth() throws SQLException {
        try (ClickHouseContainer clickhouse = new ClickHouseContainer(ClickhouseTestImages.CLICKHOUSE_24_12_IMAGE)) {
            clickhouse.start();

            ResultSet resultSet = performQuery(clickhouse, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
        }
    }
}

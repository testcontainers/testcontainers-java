package org.testcontainers.clickhouse;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.data_formats.ClickHouseBinaryFormatReader;
import com.clickhouse.client.api.query.QueryResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.ClickhouseTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ClickHouseContainerTest extends AbstractContainerDatabaseTest {

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            ClickHouseContainer clickhouse = new ClickHouseContainer("clickhouse/clickhouse-server:21.11-alpine")
            // }
        ) {
            clickhouse.start();

            performQuery(
                clickhouse,
                "SELECT 1",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            int resultSetInt = resultSet.getInt(1);
                            assertThat(resultSetInt).isEqualTo(1);
                        });
                }
            );
        }
    }

    @Test
    void customCredentialsWithUrlParams() throws SQLException {
        try (
            ClickHouseContainer clickhouse = new ClickHouseContainer("clickhouse/clickhouse-server:21.11.2-alpine")
                .withUsername("default")
                .withPassword("")
                .withDatabaseName("test")
                // The new driver uses the prefix `clickhouse_setting_` for session settings
                .withUrlParam("clickhouse_setting_max_result_rows", "5")
        ) {
            clickhouse.start();

            performQuery(
                clickhouse,
                "SELECT value FROM system.settings where name='max_result_rows'",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            int resultSetInt = resultSet.getInt(1);
                            assertThat(resultSetInt).isEqualTo(5);
                        });
                }
            );
        }
    }

    @Test
    void testNewAuth() throws SQLException {
        try (ClickHouseContainer clickhouse = new ClickHouseContainer(ClickhouseTestImages.CLICKHOUSE_24_12_IMAGE)) {
            clickhouse.start();

            performQuery(
                clickhouse,
                "SELECT 1",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            int resultSetInt = resultSet.getInt(1);
                            assertThat(resultSetInt).isEqualTo(1);
                        });
                }
            );
        }
    }

    @Test
    void testGetHttpMethodWithHttpClient() {
        try (
            ClickHouseContainer clickhouse = new ClickHouseContainer(ClickhouseTestImages.CLICKHOUSE_24_12_IMAGE);
            Client client = new Client.Builder()
                .addEndpoint(clickhouse.getHttpUrl())
                .setUsername(clickhouse.getUsername())
                .setPassword(clickhouse.getPassword())
                .build()
        ) {
            assertThatNoException()
                .isThrownBy(() -> {
                    QueryResponse queryResponse = client.query("SELECT 1").get(1, TimeUnit.MINUTES);
                    try (ClickHouseBinaryFormatReader reader = client.newBinaryFormatReader(queryResponse)) {
                        reader.next();
                        int result = reader.getInteger(1);
                        assertThat(result).isEqualTo(1);
                    }
                });
        }
    }
}

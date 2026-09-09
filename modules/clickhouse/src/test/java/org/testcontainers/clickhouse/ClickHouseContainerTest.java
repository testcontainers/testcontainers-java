package org.testcontainers.clickhouse;

import com.clickhouse.client.api.Client;
import com.clickhouse.client.api.data_formats.ClickHouseBinaryFormatReader;
import com.clickhouse.client.api.query.QueryResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.ClickhouseTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class ClickHouseContainerTest extends AbstractContainerDatabaseTest {

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            ClickHouseContainer clickhouse = new ClickHouseContainer("clickhouse/clickhouse-server:21.11-alpine")
            // }
        ) {
            clickhouse.start();

            ResultSet resultSet = performQuery(clickhouse, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
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

            ResultSet resultSet = performQuery(
                clickhouse,
                "SELECT value FROM system.settings where name='max_result_rows'"
            );

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(5);
        }
    }

    @Test
    void testNewAuth() throws SQLException {
        try (ClickHouseContainer clickhouse = new ClickHouseContainer(ClickhouseTestImages.CLICKHOUSE_24_12_IMAGE)) {
            clickhouse.start();

            ResultSet resultSet = performQuery(clickhouse, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
        }
    }

    @Test
    void testGetHttpMethodWithHttpClient() {
        ClickHouseContainer clickhouse = new ClickHouseContainer(ClickhouseTestImages.CLICKHOUSE_24_12_IMAGE);
        clickhouse.start();
        Client client = new Client.Builder()
            .addEndpoint(clickhouse.getHttpUrl())
            .setUsername(clickhouse.getUsername())
            .setPassword(clickhouse.getPassword())
            .build();
        try {
            QueryResponse queryResponse = client.query("SELECT 1").get(1, TimeUnit.MINUTES);
            ClickHouseBinaryFormatReader reader = client.newBinaryFormatReader(queryResponse);
            reader.next();
            int result = reader.getInteger(1);
            assertThat(result).isEqualTo(1);
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            fail("Cannot get sql result:" + e);
        } finally {
            clickhouse.close();
            client.close();
        }
    }

    @Test
    void testMysqlProtocol() throws SQLException {
        try (ClickHouseContainer clickhouse = new ClickHouseContainer(ClickhouseTestImages.CLICKHOUSE_24_12_IMAGE)) {
            clickhouse.start();

            assertThat(clickhouse.getMysqlPort()).isNotNull();
            assertThat(clickhouse.getHttpPort()).isNotNull();
            assertThat(clickhouse.getNativePort()).isNotNull();

            String mysqlUrl = clickhouse.getMysqlJdbcUrl();
            assertThat(mysqlUrl).startsWith("jdbc:mysql://");

            String mariadbUrl =
                "jdbc:mariadb://" +
                clickhouse.getHost() +
                ":" +
                clickhouse.getMysqlPort() +
                "/" +
                clickhouse.getDatabaseName();

            try (
                Connection connection = DriverManager.getConnection(
                    mariadbUrl,
                    clickhouse.getUsername(),
                    clickhouse.getPassword()
                );
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT 1")
            ) {
                resultSet.next();
                int resultSetInt = resultSet.getInt(1);
                assertThat(resultSetInt).isEqualTo(1);
            }
        }
    }

    @Test
    void testPostgresqlProtocol() throws SQLException {
        try (ClickHouseContainer clickhouse = new ClickHouseContainer(ClickhouseTestImages.CLICKHOUSE_24_12_IMAGE)) {
            clickhouse.start();

            assertThat(clickhouse.getPostgresqlPort()).isNotNull();

            String pgUrl = clickhouse.getPostgresqlJdbcUrl() + "?sslmode=disable&preferQueryMode=simple";
            assertThat(pgUrl).startsWith("jdbc:postgresql://");

            try (
                Connection connection = DriverManager.getConnection(
                    pgUrl,
                    clickhouse.getUsername(),
                    clickhouse.getPassword()
                );
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT 1")
            ) {
                resultSet.next();
                int resultSetInt = resultSet.getInt(1);
                assertThat(resultSetInt).isEqualTo(1);
            }
        }
    }
}

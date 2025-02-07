package org.testcontainers.clickhouse;

import com.zaxxer.hikari.pool.PoolInitializationException;
import org.junit.Test;
import org.testcontainers.ClickhouseTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ClickHouseContainerTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        for (DockerImageName image : Arrays.asList(ClickhouseTestImages.CLICKHOUSE_IMAGE, ClickhouseTestImages.CLICKHOUSE_24_12_IMAGE)) {
            try (ClickHouseContainer clickhouse = new ClickHouseContainer(image)) {
                clickhouse.start();

                ResultSet resultSet = performQuery(clickhouse, "SELECT 1");

                int resultSetInt = resultSet.getInt(1);
                assertThat(resultSetInt).isEqualTo(1);
            }
        }
    }

    @Test
    public void customCredentialsWithUrlParams() throws SQLException {
        for (DockerImageName image : Arrays.asList(ClickhouseTestImages.CLICKHOUSE_IMAGE, ClickhouseTestImages.CLICKHOUSE_24_12_IMAGE)) {
            try (
                ClickHouseContainer clickhouse = new ClickHouseContainer(image)
                    .withUsername("test")
                    .withPassword("test")
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
    }

    @Test
    public void testNewAuth() {
        try (ClickHouseContainer clickhouse = new ClickHouseContainer(ClickhouseTestImages.CLICKHOUSE_24_12_IMAGE)
            .withUsername("default").withPassword("")) {
            clickhouse.start();

            PoolInitializationException exception = assertThrows(PoolInitializationException.class, () -> performQuery(clickhouse, "SELECT 1"));
            Throwable cause = exception.getCause();
            assertTrue(cause instanceof SQLException);
            assertTrue(cause.getMessage().contains("Authentication failed: password is incorrect, or there is no user with such name."));
        }
    }
}

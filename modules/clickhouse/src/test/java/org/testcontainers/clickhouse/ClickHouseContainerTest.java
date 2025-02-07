package org.testcontainers.clickhouse;

import com.zaxxer.hikari.pool.PoolInitializationException;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void testNewAuth() throws SQLException {
        try (ClickHouseContainer clickhouse = new ClickHouseContainer("clickhouse/clickhouse-server:24.12-alpine")
            .withUsername("default").withPassword("")) {
            clickhouse.start();

            PoolInitializationException exception = assertThrows(PoolInitializationException.class, () -> performQuery(clickhouse, "SELECT 1"));
            Throwable cause = exception.getCause();
            assertTrue(cause instanceof SQLException);
            assertTrue(cause.getMessage().contains("Authentication failed: password is incorrect, or there is no user with such name."));
        }
    }
}

package org.testcontainers.junit.clickhouse;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.ClickhouseTestImages;
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleClickhouseTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (ClickHouseContainer clickhouse = new ClickHouseContainer(ClickhouseTestImages.CLICKHOUSE_IMAGE)) {
            clickhouse.start();

            performQuery(
                clickhouse,
                "SELECT 1",
                resultSet -> {
                    Assertions
                        .assertThatNoException()
                        .isThrownBy(() -> {
                            int resultSetInt = resultSet.getInt(1);
                            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
                        });
                }
            );
        }
    }
}

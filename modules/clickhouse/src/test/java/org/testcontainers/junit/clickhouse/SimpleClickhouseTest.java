package org.testcontainers.junit.clickhouse;

import org.junit.jupiter.api.Test;
import org.testcontainers.ClickhouseTestImages;
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.SQLException;

class SimpleClickhouseTest extends AbstractContainerDatabaseTest {

    @Test
    void testSimple() throws SQLException {
        try (ClickHouseContainer clickhouse = new ClickHouseContainer(ClickhouseTestImages.CLICKHOUSE_IMAGE)) {
            clickhouse.start();

            executeSelectOneQuery(clickhouse);
        }
    }
}

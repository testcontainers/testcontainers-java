package org.testcontainers.junit.tidb;

import org.junit.Test;
import org.testcontainers.TiDBTestImages;
import org.testcontainers.containers.TiDBContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class SimpleTiDBTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (TiDBContainer tidb = new TiDBContainer(TiDBTestImages.TIDB_IMAGE)) {
            tidb.start();

            ResultSet resultSet = performQuery(tidb, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (
            TiDBContainer tidb = new TiDBContainer(TiDBTestImages.TIDB_IMAGE).withInitScript("somepath/init_mysql.sql")
        ) { // TiDB is expected to be compatible with MySQL
            tidb.start();

            ResultSet resultSet = performQuery(tidb, "SELECT foo FROM bar");

            String firstColumnValue = resultSet.getString(1);
            assertEquals("Value from init script should equal real value", "hello world", firstColumnValue);
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        TiDBContainer tidb = new TiDBContainer(TiDBTestImages.TIDB_IMAGE).withUrlParam("sslmode", "disable");

        try {
            tidb.start();
            String jdbcUrl = tidb.getJdbcUrl();
            assertThat(jdbcUrl, containsString("?"));
            assertThat(jdbcUrl, containsString("sslmode=disable"));
        } finally {
            tidb.stop();
        }
    }
}

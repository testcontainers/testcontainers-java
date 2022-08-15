package org.testcontainers.junit.tidb;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.TiDBTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.tidb.TiDBContainer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SimpleTiDBTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (TiDBContainer tidb = new TiDBContainer(TiDBTestImages.TIDB_IMAGE)) {
            tidb.start();

            ResultSet resultSet = performQuery(tidb, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            Assert.assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (
            TiDBContainer tidb = new TiDBContainer(TiDBTestImages.TIDB_IMAGE).withInitScript("somepath/init_tidb.sql")
        ) { // TiDB is expected to be compatible with MySQL
            tidb.start();

            ResultSet resultSet = performQuery(tidb, "SELECT foo FROM bar");

            String firstColumnValue = resultSet.getString(1);
            Assert.assertEquals("Value from init script should equal real value", "hello world", firstColumnValue);
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        TiDBContainer tidb = new TiDBContainer(TiDBTestImages.TIDB_IMAGE).withUrlParam("sslmode", "disable");

        try {
            tidb.start();
            String jdbcUrl = tidb.getJdbcUrl();
            Assert.assertThat(jdbcUrl, Matchers.containsString("?"));
            Assert.assertThat(jdbcUrl, Matchers.containsString("sslmode=disable"));
        } finally {
            tidb.stop();
        }
    }
}

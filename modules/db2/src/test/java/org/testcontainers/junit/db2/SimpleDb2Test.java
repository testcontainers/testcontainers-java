package org.testcontainers.junit.db2;

import org.junit.Test;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.testcontainers.Db2TestImages.DB2_IMAGE;

public class SimpleDb2Test extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (Db2Container db2 = new Db2Container(DB2_IMAGE)
            .acceptLicense()) {

            db2.start();

            ResultSet resultSet = performQuery(db2, "SELECT 1 FROM SYSIBM.SYSDUMMY1");

            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        try (Db2Container db2 = new Db2Container(DB2_IMAGE)
            .withUrlParam("sslConnection", "false")
            .acceptLicense()) {

            db2.start();

            String jdbcUrl = db2.getJdbcUrl();
            assertThat(jdbcUrl, containsString(":sslConnection=false;"));
        }
    }
}

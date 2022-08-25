package org.testcontainers.junit.db2;

import org.junit.Test;
import org.testcontainers.Db2TestImages;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleDb2ContainerTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (Db2Container db2 = new Db2Container(Db2TestImages.DB2_IMAGE).acceptLicense()) {
            db2.start();

            ResultSet resultSet = performQuery(db2, "SELECT 1 FROM SYSIBM.SYSDUMMY1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        try (
            Db2Container db2 = new Db2Container(Db2TestImages.DB2_IMAGE)
                .withUrlParam("sslConnection", "false")
                .acceptLicense()
        ) {
            db2.start();

            String jdbcUrl = db2.getJdbcUrl();
            assertThat(jdbcUrl).contains(":sslConnection=false;");
        }
    }

    @Test
    public void testExposedAndLivenessCheckPorts() {
        try (Db2Container db2 = new Db2Container(Db2TestImages.DB2_IMAGE).acceptLicense()) {
            db2.start();
            assertThat(db2.getExposedPorts()).containsExactly(50_000);
            assertThat(db2.getLivenessCheckPortNumbers()).containsExactly(db2.getMappedPort(50_000));
        }
    }
}

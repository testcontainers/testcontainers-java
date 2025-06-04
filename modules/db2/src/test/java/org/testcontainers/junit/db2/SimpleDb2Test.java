package org.testcontainers.junit.db2;

import org.junit.jupiter.api.Test;
import org.testcontainers.Db2TestImages;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleDb2Test extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (Db2Container db2 = new Db2Container(Db2TestImages.DB2_IMAGE).acceptLicense()) {
            db2.start();

            ResultSet resultSet = performQuery(db2, "SELECT 1 FROM SYSIBM.SYSDUMMY1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(db2);
        }
    }

    @Test
    public void testSimpleWithNewImage() throws SQLException {
        try (Db2Container db2 = new Db2Container("icr.io/db2_community/db2:11.5.8.0").acceptLicense()) {
            db2.start();

            ResultSet resultSet = performQuery(db2, "SELECT 1 FROM SYSIBM.SYSDUMMY1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(db2);
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

    private void assertHasCorrectExposedAndLivenessCheckPorts(Db2Container db2) {
        assertThat(db2.getExposedPorts()).containsExactly(Db2Container.DB2_PORT);
        assertThat(db2.getLivenessCheckPortNumbers()).containsExactly(db2.getMappedPort(Db2Container.DB2_PORT));
    }
}

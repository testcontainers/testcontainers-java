package org.testcontainers.db2;

import org.junit.jupiter.api.Test;
import org.testcontainers.Db2TestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class Db2ContainerTest extends AbstractContainerDatabaseTest {

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            Db2Container db2 = new Db2Container("icr.io/db2_community/db2:11.5.8.0").acceptLicense()
            // }
        ) {
            db2.start();

            ResultSet resultSet = performQuery(db2, "SELECT 1 FROM SYSIBM.SYSDUMMY1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(db2);
        }
    }

    @Test
    void testSimpleWithNewImage() throws SQLException {
        try (Db2Container db2 = new Db2Container("icr.io/db2_community/db2:11.5.8.0").acceptLicense()) {
            db2.start();

            ResultSet resultSet = performQuery(db2, "SELECT 1 FROM SYSIBM.SYSDUMMY1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(db2);
        }
    }

    @Test
    void testWithAdditionalUrlParamInJdbcUrl() {
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

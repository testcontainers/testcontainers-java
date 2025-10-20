package org.testcontainers.databend;

import org.junit.jupiter.api.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class DatabendContainerTest extends AbstractContainerDatabaseTest {

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            DatabendContainer databend = new DatabendContainer("datafuselabs/databend:v1.2.615")
            // }
        ) {
            databend.start();

            ResultSet resultSet = performQuery(databend, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
        }
    }

    @Test
    void customCredentialsWithUrlParams() throws SQLException {
        try (
            DatabendContainer databend = new DatabendContainer("datafuselabs/databend:v1.2.615")
                .withUsername("test")
                .withPassword("test")
                .withUrlParam("ssl", "false")
        ) {
            databend.start();

            ResultSet resultSet = performQuery(databend, "SELECT 1;");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
        }
    }
}

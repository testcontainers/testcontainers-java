package org.testcontainers.databend;

import org.junit.jupiter.api.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabendContainerTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (DatabendContainer databend = new DatabendContainer("datafuselabs/databend:v1.2.615")) {
            databend.start();

            ResultSet resultSet = performQuery(databend, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
        }
    }

    @Test
    public void customCredentialsWithUrlParams() throws SQLException {
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

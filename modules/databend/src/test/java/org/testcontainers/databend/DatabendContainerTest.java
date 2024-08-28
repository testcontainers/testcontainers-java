package org.testcontainers.databend;

import org.junit.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class DatabendContainerTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (DatabendContainer databend = new DatabendContainer("datafuselabs/databend:v1.2.623-nightly")) {
            databend.start();

            ResultSet resultSet = performQuery(databend, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
            assertThat(databend.getJdbcUrl).contains("?ssl=false");
        }
    }

    @Test
    public void customCredentialsWithUrlParams() throws SQLException {
        try (
            DatabendContainer databend = new DatabendContainer("datafuselabs/databend:v1.2.623-nightly")
                .withUsername("databend")
                .withPassword("databend")
                .withDatabaseName("default")
                .withUrlParam("ssl", false)
        ) {
            databend.start();

            ResultSet resultSet = performQuery(
                databend,
                "SELECT 1'"
            );

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
        }
    }
}

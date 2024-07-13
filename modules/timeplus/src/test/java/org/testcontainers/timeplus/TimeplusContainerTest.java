package org.testcontainers.timeplus;

import org.junit.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeplusContainerTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (TimeplusContainer timeplus = new TimeplusContainer("timeplus/timeplusd:2.3.3")) {
            timeplus.start();

            ResultSet resultSet = performQuery(timeplus, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
        }
    }

    @Test
    public void customCredentialsWithUrlParams() throws SQLException {
        try (
            TimeplusContainer timeplus = new TimeplusContainer("timeplus/timeplusd:2.3.3")
                .withUsername("system")
                .withPassword("sys@t+")
                .withDatabaseName("system")
                .withUrlParam("max_result_rows", "5")
        ) {
            timeplus.start();

            ResultSet resultSet = performQuery(
                timeplus,
                "SELECT value FROM system.settings where name='max_result_rows'"
            );

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(5);
        }
    }
}

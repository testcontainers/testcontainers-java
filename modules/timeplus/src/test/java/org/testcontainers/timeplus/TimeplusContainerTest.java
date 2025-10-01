package org.testcontainers.timeplus;

import org.junit.jupiter.api.Test;
import org.testcontainers.TimeplusImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class TimeplusContainerTest extends AbstractContainerDatabaseTest {

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            TimeplusContainer timeplus = new TimeplusContainer("timeplus/timeplusd:2.3.21")
            // }
        ) {
            timeplus.start();

            ResultSet resultSet = performQuery(timeplus, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
        }
    }

    @Test
    void customCredentialsWithUrlParams() throws SQLException {
        try (
            TimeplusContainer timeplus = new TimeplusContainer(TimeplusImages.TIMEPLUS_IMAGE)
                .withUsername("system")
                .withPassword("sys@t+")
                .withDatabaseName("system")
                .withUrlParam("interactive_delay", "5")
        ) {
            timeplus.start();

            ResultSet resultSet = performQuery(
                timeplus,
                "SELECT to_int(value) FROM system.settings where name='interactive_delay'"
            );

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(5);
        }
    }
}

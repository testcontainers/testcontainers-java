package org.testcontainers.timeplus;

import org.junit.Test;
import org.testcontainers.TimeplusImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class TimeplusContainerTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (TimeplusContainer timeplus = new TimeplusContainer(TimeplusImages.TIMEPLUS_IMAGE)) {
            timeplus.start();

            ResultSet resultSet = performQuery(timeplus, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).isEqualTo(1);
        }
    }

    @Test
    public void customCredentialsWithUrlParams() throws SQLException {
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

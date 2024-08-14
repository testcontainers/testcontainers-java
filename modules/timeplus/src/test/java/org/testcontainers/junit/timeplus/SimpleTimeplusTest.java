package org.testcontainers.junit.timeplus;

import org.junit.Test;
import org.testcontainers.TimeplusImages;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.timeplus.TimeplusContainerProvider;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleTimeplusTest extends AbstractContainerDatabaseTest {

    public static Object[][] data() {
        return new Object[][] { { TimeplusImages.TIMEPLUS_IMAGE } };
    }

    @Test
    public void testSimple() throws SQLException {
        try (JdbcDatabaseContainer timeplus = new TimeplusContainerProvider().newInstance()) {
            timeplus.start();

            ResultSet resultSet = performQuery(timeplus, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }
}

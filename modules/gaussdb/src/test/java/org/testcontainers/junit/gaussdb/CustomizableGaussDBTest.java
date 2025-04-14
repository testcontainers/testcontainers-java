package org.testcontainers.junit.gaussdb;

import org.junit.Test;
import org.testcontainers.GaussDBTestImages;
import org.testcontainers.containers.GaussDBContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class CustomizableGaussDBTest extends AbstractContainerDatabaseTest {

    private static final String DB_NAME = "foo";

    private static final String USER = "bar";

    private static final String PWD = "GaussDB@123";

    @Test
    public void testSimple() throws SQLException {
        try (
            GaussDBContainer<?> postgres = new GaussDBContainer<>(GaussDBTestImages.GAUSSDB_TEST_IMAGE)
                .withDatabaseName(DB_NAME)
                .withUsername(USER)
                .withPassword(PWD)
        ) {
            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }
}

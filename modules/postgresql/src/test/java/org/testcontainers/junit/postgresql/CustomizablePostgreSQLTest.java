package org.testcontainers.junit.postgresql;

import org.junit.Test;
import org.testcontainers.PostgreSQLTestImages;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

/**
 * @author richardnorth
 */
public class CustomizablePostgreSQLTest extends AbstractContainerDatabaseTest {
    private static final String DB_NAME = "foo";
    private static final String USER = "bar";
    private static final String PWD = "baz";

    @Test
    public void testSimple() throws SQLException {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PostgreSQLTestImages.POSTGRES_TEST_IMAGE)
                .withDatabaseName(DB_NAME)
                .withUsername(USER)
                .withPassword(PWD)) {

            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }
}

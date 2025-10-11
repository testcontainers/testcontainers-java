package org.testcontainers.junit.postgresql;

import org.junit.jupiter.api.Test;
import org.testcontainers.PostgreSQLTestImages;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class CustomizablePostgreSQLTest extends AbstractContainerDatabaseTest {

    private static final String DB_NAME = "foo";

    private static final String USER = "bar";

    private static final String PWD = "baz";

    @Test
    void testSimple() throws SQLException {
        try (
            PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(PostgreSQLTestImages.POSTGRES_TEST_IMAGE)
                .withDatabaseName(DB_NAME)
                .withUsername(USER)
                .withPassword(PWD)
        ) {
            postgres.start();

            performQuery(
                postgres,
                "SELECT 1",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            int resultSetInt = resultSet.getInt(1);
                            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
                        });
                }
            );
        }
    }
}

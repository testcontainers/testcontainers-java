package org.testcontainers.junit.mysql;

import org.junit.jupiter.api.Test;
import org.testcontainers.MySQLTestImages;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class CustomizableMysqlTest extends AbstractContainerDatabaseTest {

    private static final String DB_NAME = "foo";

    private static final String USER = "bar";

    private static final String PWD = "baz";

    @Test
    void testSimple() throws SQLException {
        // Add MYSQL_ROOT_HOST environment so that we can root login from anywhere for testing purposes
        try (
            MySQLContainer<?> mysql = new MySQLContainer<>(MySQLTestImages.MYSQL_80_IMAGE)
                .withDatabaseName(DB_NAME)
                .withUsername(USER)
                .withPassword(PWD)
                .withEnv("MYSQL_ROOT_HOST", "%")
        ) {
            mysql.start();

            performQuery(
                mysql,
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

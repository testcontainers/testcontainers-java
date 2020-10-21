package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.testcontainers.PostgreSQLTestImages.POSTGRES_ALPINE_VERSION;
import static org.testcontainers.PostgreSQLTestImages.POSTGRES_VERSION;

public class InitializedDatabasePostgresTest extends AbstractContainerDatabaseTest {

    @Test
    public void testAlpine() throws SQLException {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder ->
                    builder
                        .from(POSTGRES_ALPINE_VERSION)
                        .copy("/tmp/data", "/var/lib/postgresql/data")
                        .build()
                )
                .withFileFromClasspath("/tmp/data", "data")
            )) {

            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testDefault() throws SQLException {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder ->
                    builder
                        .from(POSTGRES_VERSION)
                        .copy("/tmp/data", "/var/lib/postgresql/data")
                        .build()
                )
                .withFileFromClasspath("/tmp/data", "data")
        )) {

            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }
}

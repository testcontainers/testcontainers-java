package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class CompatibleImageTest extends AbstractContainerDatabaseTest {

    @Test
    public void pgvector() throws SQLException {
        try (
            // pgvectorContainer {
            PostgreSQLContainer<?> pgvector = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            // }
        ) {
            pgvector.start();

            ResultSet resultSet = performQuery(pgvector, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }

    @Test
    public void postgis() throws SQLException {
        try (
            // postgisContainer {
            PostgreSQLContainer<?> postgis = new PostgreSQLContainer<>(
                DockerImageName.parse("postgis/postgis:16-3.4-alpine").asCompatibleSubstituteFor("postgres")
            )
            // }
        ) {
            postgis.start();

            ResultSet resultSet = performQuery(postgis, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }

    @Test
    public void timescaledb() throws SQLException {
        try (
            // timescaledbContainer {
            PostgreSQLContainer<?> timescaledb = new PostgreSQLContainer<>(
                DockerImageName.parse("timescale/timescaledb:2.14.2-pg16").asCompatibleSubstituteFor("postgres")
            )
            // }
        ) {
            timescaledb.start();

            ResultSet resultSet = performQuery(timescaledb, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }
}

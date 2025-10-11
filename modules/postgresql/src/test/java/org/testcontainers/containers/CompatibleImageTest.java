package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class CompatibleImageTest extends AbstractContainerDatabaseTest {

    @Test
    void pgvector() throws SQLException {
        try (
            // pgvectorContainer {
            PostgreSQLContainer<?> pgvector = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            // }
        ) {
            pgvector.start();

            performQuery(
                pgvector,
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

    @Test
    void postgis() throws SQLException {
        try (
            // postgisContainer {
            PostgreSQLContainer<?> postgis = new PostgreSQLContainer<>(
                DockerImageName.parse("postgis/postgis:16-3.4-alpine").asCompatibleSubstituteFor("postgres")
            )
            // }
        ) {
            postgis.start();

            performQuery(
                postgis,
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

    @Test
    void timescaledb() throws SQLException {
        try (
            // timescaledbContainer {
            PostgreSQLContainer<?> timescaledb = new PostgreSQLContainer<>(
                DockerImageName.parse("timescale/timescaledb:2.14.2-pg16").asCompatibleSubstituteFor("postgres")
            )
            // }
        ) {
            timescaledb.start();

            performQuery(
                timescaledb,
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

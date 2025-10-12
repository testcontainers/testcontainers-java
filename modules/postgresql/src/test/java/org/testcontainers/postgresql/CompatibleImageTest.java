package org.testcontainers.postgresql;

import org.junit.jupiter.api.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.SQLException;

class CompatibleImageTest extends AbstractContainerDatabaseTest {

    @Test
    void pgvector() throws SQLException {
        try (
            // pgvectorContainer {
            PostgreSQLContainer pgvector = new PostgreSQLContainer("pgvector/pgvector:pg16")
            // }
        ) {
            pgvector.start();

            performSelectOneQuery(pgvector);
        }
    }

    @Test
    void postgis() throws SQLException {
        try (
            // postgisContainer {
            PostgreSQLContainer postgis = new PostgreSQLContainer(
                DockerImageName.parse("postgis/postgis:16-3.4-alpine").asCompatibleSubstituteFor("postgres")
            )
            // }
        ) {
            postgis.start();

            performSelectOneQuery(postgis);
        }
    }

    @Test
    void timescaledb() throws SQLException {
        try (
            // timescaledbContainer {
            PostgreSQLContainer timescaledb = new PostgreSQLContainer(
                DockerImageName.parse("timescale/timescaledb:2.14.2-pg16").asCompatibleSubstituteFor("postgres")
            )
            // }
        ) {
            timescaledb.start();

            performSelectOneQuery(timescaledb);
        }
    }
}

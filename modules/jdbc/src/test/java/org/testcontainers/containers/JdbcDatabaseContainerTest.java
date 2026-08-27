package org.testcontainers.containers;

import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

class JdbcDatabaseContainerTest {

    @Test
    void testGetR2dbcUrlWithDifferentJdbcUrls() {
        // Null URL
        JdbcDatabaseContainer<?> nullUrlContainer = new JdbcDatabaseContainerStub("mysql:latest") {
            @Override
            public String getJdbcUrl() {
                return null;
            }
        };
        assertThat(nullUrlContainer.getR2dbcUrl()).isNull();

        // Standard postgres URL
        JdbcDatabaseContainer<?> postgresContainer = new JdbcDatabaseContainerStub("postgres:latest") {
            @Override
            public String getJdbcUrl() {
                return "jdbc:postgresql://localhost:5432/testdb";
            }
        };
        assertThat(postgresContainer.getR2dbcUrl())
            .isEqualTo("r2dbc:postgresql://localhost:5432/testdb");

        // MSSQL server URL
        JdbcDatabaseContainer<?> mssqlContainer = new JdbcDatabaseContainerStub("mssql:latest") {
            @Override
            public String getJdbcUrl() {
                return "jdbc:sqlserver://localhost:1433;databaseName=testdb";
            }
        };
        assertThat(mssqlContainer.getR2dbcUrl())
            .isEqualTo("r2dbc:mssql://localhost:1433;databaseName=testdb");

        // Oracle server URL
        JdbcDatabaseContainer<?> oracleContainer = new JdbcDatabaseContainerStub("oracle:latest") {
            @Override
            public String getJdbcUrl() {
                return "jdbc:oracle:thin:@localhost:1521/xepdb1";
            }
        };
        assertThat(oracleContainer.getR2dbcUrl())
            .isEqualTo("r2dbc:oracle://localhost:1521/xepdb1");
    }

    @Test
    void anExceptionIsThrownIfJdbcIsNotAvailable() {
        JdbcDatabaseContainer<?> jdbcContainer = new JdbcDatabaseContainerStub("mysql:latest")
            .withStartupTimeoutSeconds(1);

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(jdbcContainer::waitUntilContainerStarted);
    }

    static class JdbcDatabaseContainerStub extends JdbcDatabaseContainer {

        public JdbcDatabaseContainerStub(@NonNull String dockerImageName) {
            super(dockerImageName);
        }

        @Override
        public String getDriverClassName() {
            return null;
        }

        @Override
        public String getJdbcUrl() {
            return null;
        }

        @Override
        public String getUsername() {
            return null;
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        protected String getTestQueryString() {
            return null;
        }

        @Override
        public boolean isRunning() {
            return true;
        }

        @Override
        public Connection createConnection(String queryString) throws SQLException, NoDriverFoundException {
            throw new SQLException("Could not create new connection");
        }

        @Override
        protected Logger logger() {
            return mock(Logger.class);
        }

        @Override
        public void setDockerImageName(@NonNull String dockerImageName) {}
    }
}

package org.testcontainers.containers;

import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

class JdbcDatabaseContainerTest {

    @Test
    void anExceptionIsThrownIfJdbcIsNotAvailable() {
        JdbcDatabaseContainer<?> jdbcContainer = new JdbcDatabaseContainerStub("mysql:latest")
            .withStartupTimeoutSeconds(1);

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(jdbcContainer::waitUntilContainerStarted);
    }

    @Test
    void getR2dbcUrlReturnsR2dbcPrefixedUrl() {
        JdbcDatabaseContainer<?> jdbcContainer = new JdbcDatabaseContainerStub("mysql:latest") {
            @Override
            public String getJdbcUrl() {
                return "jdbc:mysql://localhost:3306/test";
            }
        };

        String r2dbcUrl = jdbcContainer.getR2dbcUrl();
        assert r2dbcUrl.equals("r2dbc:mysql://localhost:3306/test");
    }

    @Test
    void getR2dbcUrlThrowsExceptionIfJdbcPrefixMissing() {
        JdbcDatabaseContainer<?> jdbcContainer = new JdbcDatabaseContainerStub("mysql:latest") {
            @Override
            public String getJdbcUrl() {
                return "mysql://localhost:3306/test";
            }
        };

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(jdbcContainer::getR2dbcUrl);
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

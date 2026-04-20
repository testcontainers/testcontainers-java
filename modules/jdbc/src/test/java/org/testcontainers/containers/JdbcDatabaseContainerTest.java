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
    void getR2dbcUrlConvertsJdbcUrlToR2dbcFormat() {
        JdbcDatabaseContainer<?> container = new JdbcDatabaseContainerStubWithUrl(
            "mysql:latest",
            "jdbc:mysql://localhost:3306/testdb"
        );

        String r2dbcUrl = container.getR2dbcUrl();

        assertThat(r2dbcUrl).isEqualTo("r2dbc:mysql://localhost:3306/testdb");
    }

    @Test
    void getR2dbcUrlThrowsExceptionWhenJdbcUrlIsNull() {
        JdbcDatabaseContainer<?> container = new JdbcDatabaseContainerStub("mysql:latest");

        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(container::getR2dbcUrl)
            .withMessageContaining("Cannot convert JDBC URL to R2DBC format");
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

    static class JdbcDatabaseContainerStubWithUrl extends JdbcDatabaseContainerStub {

        private final String jdbcUrl;

        public JdbcDatabaseContainerStubWithUrl(@NonNull String dockerImageName, String jdbcUrl) {
            super(dockerImageName);
            this.jdbcUrl = jdbcUrl;
        }

        @Override
        public String getJdbcUrl() {
            return jdbcUrl;
        }
    }
}

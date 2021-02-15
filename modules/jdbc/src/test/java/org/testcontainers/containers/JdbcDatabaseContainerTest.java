package org.testcontainers.containers;

import java.sql.Connection;
import java.sql.SQLException;
import lombok.NonNull;
import org.junit.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

public class JdbcDatabaseContainerTest {

    @Test
    public void anExceptionIsThrownIfJdbcIsNotAvailable() {
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

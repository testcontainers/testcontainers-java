package org.testcontainers.jdbc;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MissingJdbcDriverTest {

    @Test
    void shouldFailFastIfNoDriverFound() {
        try (MissingDriverContainer container = new MissingDriverContainer()) {
            Throwable thrown = Assertions.catchThrowable(container::start);
            Throwable rootCause = org.assertj.core.util.Throwables.getRootCause(thrown);

            Assertions
                .assertThat(rootCause)
                .as("Root cause should be ClassNotFoundException")
                .isInstanceOf(ClassNotFoundException.class);

            assertThat(container.getConnectionAttempts())
                .as("only one connection attempt should have been made")
                .isEqualTo(1);
        }
    }

    /**
     * Container class for the purposes of testing, with a known non-existent driver
     */
    static class MissingDriverContainer extends JdbcDatabaseContainer {

        private final AtomicInteger connectionAttempts = new AtomicInteger();

        MissingDriverContainer() {
            super(DockerImageName.parse("mysql:8.0.36"));
            withEnv("MYSQL_ROOT_PASSWORD", "test");
            withExposedPorts(3306);
        }

        @Override
        public String getDriverClassName() {
            return "nonexistent.ClassName";
        }

        @Override
        public String getJdbcUrl() {
            return "";
        }

        @Override
        public String getUsername() {
            return "root";
        }

        @Override
        public String getPassword() {
            return "test";
        }

        @Override
        protected String getTestQueryString() {
            return "";
        }

        @Override
        public Connection createConnection(String queryString) throws SQLException, NoDriverFoundException {
            connectionAttempts.incrementAndGet(); //
            return super.createConnection(queryString);
        }

        /**
         * test window
         * @return how many times a connection was attempted
         */
        int getConnectionAttempts() {
            return connectionAttempts.get();
        }
    }
}

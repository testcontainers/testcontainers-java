package org.testcontainers.containers;

import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcDatabaseContainerTest {

    @Test
    void anExceptionIsThrownIfJdbcIsNotAvailable() {
        JdbcDatabaseContainer<?> jdbcContainer = new JdbcDatabaseContainerStub("mysql:latest")
            .withStartupTimeoutSeconds(1);

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(jdbcContainer::waitUntilContainerStarted);
    }

    @Test
    void testQueryIsRetriedIfReadingItsResultsFails() {
        // Some databases (e.g. Trino) accept the test query but only fail once its results are fetched
        FailingResultSetJdbcDatabaseContainerStub jdbcContainer = new FailingResultSetJdbcDatabaseContainerStub();
        jdbcContainer.withStartupTimeoutSeconds(5);

        jdbcContainer.waitUntilContainerStarted();

        assertThat(jdbcContainer.connectionAttempts).hasValue(2);
    }

    @Test
    void testQueryTimeoutUsesRemainingStartupTime() throws SQLException {
        Statement statement = mock(Statement.class);
        when(statement.execute("SELECT 1")).thenReturn(true);
        JdbcDatabaseContainer<?> jdbcContainer = containerWithStatement(statement).withStartupTimeoutSeconds(2);

        jdbcContainer.waitUntilContainerStarted();

        verify(statement).setQueryTimeout(1);
    }

    @Test
    void unsupportedQueryTimeoutDoesNotPreventStartup() throws SQLException {
        Statement statement = mock(Statement.class);
        doThrow(new SQLFeatureNotSupportedException()).when(statement).setQueryTimeout(anyInt());
        when(statement.execute("SELECT 1")).thenReturn(true);

        containerWithStatement(statement).waitUntilContainerStarted();

        verify(statement).execute("SELECT 1");
    }

    @Test
    void resultsCompletedAfterStartupTimeoutDoNotReportReady() throws SQLException {
        Statement statement = mock(Statement.class);
        when(statement.execute("SELECT 1")).thenReturn(true);
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.getResultSet()).thenReturn(resultSet);
        when(resultSet.next())
            .thenAnswer(invocation -> {
                Thread.sleep(1100);
                return false;
            });
        JdbcDatabaseContainer<?> jdbcContainer = containerWithStatement(statement).withStartupTimeoutSeconds(1);

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(jdbcContainer::waitUntilContainerStarted)
            .withCauseInstanceOf(SQLTimeoutException.class);

        verify(resultSet).close();
        verify(statement).close();
    }

    @Test
    void resultDrainingStopsAfterStartupTimeout() throws SQLException {
        Statement statement = mock(Statement.class);
        when(statement.execute("SELECT 1")).thenReturn(true);
        ResultSet resultSet = mock(ResultSet.class);
        when(statement.getResultSet()).thenReturn(resultSet);
        when(resultSet.next())
            .thenAnswer(invocation -> {
                Thread.sleep(1100);
                return true;
            })
            .thenReturn(false);
        JdbcDatabaseContainer<?> jdbcContainer = containerWithStatement(statement).withStartupTimeoutSeconds(1);

        assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(jdbcContainer::waitUntilContainerStarted)
            .withCauseInstanceOf(SQLTimeoutException.class);

        verify(resultSet).next();
        verify(resultSet).close();
        verify(statement).close();
    }

    private JdbcDatabaseContainer<?> containerWithStatement(Statement statement) throws SQLException {
        Connection connection = mock(Connection.class);
        when(connection.createStatement()).thenReturn(statement);
        return new JdbcDatabaseContainerStub("mysql:latest") {
            @Override
            protected String getTestQueryString() {
                return "SELECT 1";
            }

            @Override
            public Connection createConnection(String queryString) {
                return connection;
            }
        };
    }

    static class FailingResultSetJdbcDatabaseContainerStub extends JdbcDatabaseContainerStub {

        private final AtomicInteger connectionAttempts = new AtomicInteger();

        FailingResultSetJdbcDatabaseContainerStub() {
            super("mysql:latest");
        }

        @Override
        protected String getTestQueryString() {
            return "SELECT 1";
        }

        @Override
        public Connection createConnection(String queryString) throws SQLException, NoDriverFoundException {
            ResultSet resultSet = mock(ResultSet.class);
            if (connectionAttempts.incrementAndGet() == 1) {
                when(resultSet.next()).thenThrow(new SQLException("No nodes available to run query"));
            } else {
                when(resultSet.next()).thenReturn(true, false);
            }
            Statement statement = mock(Statement.class);
            when(statement.execute("SELECT 1")).thenReturn(true);
            when(statement.getResultSet()).thenReturn(resultSet);
            Connection connection = mock(Connection.class);
            when(connection.createStatement()).thenReturn(statement);
            return connection;
        }
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

package org.testcontainers.containers;

import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.testcontainers.delegate.DatabaseDelegate;

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JdbcDatabaseContainerTest {

    @Test
    void anExceptionIsThrownIfJdbcIsNotAvailable() {
        JdbcDatabaseContainer<?> jdbcContainer = new JdbcDatabaseContainerStub("mysql:latest")
            .withStartupTimeoutSeconds(1);

        assertThatExceptionOfType(IllegalStateException.class).isThrownBy(jdbcContainer::waitUntilContainerStarted);
    }

    @Test
    void readsInitScriptAsUtf8ByDefault() {
        JdbcDatabaseContainerStub container = new JdbcDatabaseContainerStub("mysql:latest");
        container.withInitScript("init-utf-8.sql");

        container.runInitScriptIfRequired();

        verify(container.delegate).execute(Collections.singletonList("SELECT 'café'"), "init-utf-8.sql", false, false);
    }

    @Test
    void readsInitScriptWithConfiguredCharset() {
        JdbcDatabaseContainerStub container = new JdbcDatabaseContainerStub("mysql:latest");
        assertThat(container.withInitScriptCharset(Charset.forName("windows-1252"))).isSameAs(container);
        container.withInitScript("init-windows-1252.sql");

        container.runInitScriptIfRequired();

        verify(container.delegate)
            .execute(Collections.singletonList("SELECT 'café'"), "init-windows-1252.sql", false, false);
    }

    @Test
    void readsAllInitScriptsWithConfiguredCharset() {
        JdbcDatabaseContainerStub container = new JdbcDatabaseContainerStub("mysql:latest");
        container
            .withInitScripts("init-windows-1252.sql", null, "init-windows-1252.sql")
            .withInitScriptCharset(Charset.forName("windows-1252"));

        container.runInitScriptIfRequired();

        verify(container.delegate, times(2))
            .execute(Collections.singletonList("SELECT 'café'"), "init-windows-1252.sql", false, false);
    }

    @Test
    void rejectsNullInitScriptCharset() {
        JdbcDatabaseContainerStub container = new JdbcDatabaseContainerStub("mysql:latest");

        assertThatExceptionOfType(NullPointerException.class).isThrownBy(() -> container.withInitScriptCharset(null));
    }

    static class JdbcDatabaseContainerStub extends JdbcDatabaseContainer<JdbcDatabaseContainerStub> {

        private final DatabaseDelegate delegate = mock(DatabaseDelegate.class);

        @Override
        protected DatabaseDelegate getDatabaseDelegate() {
            return delegate;
        }

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

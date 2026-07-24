package org.testcontainers.containers;

import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
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
    void createConnectionForwardsInfoPropertiesToTheDriverAsRealEntriesNotJustDefaults() throws Exception {
        CapturingDriver.lastReceivedProperties = null;

        JdbcDatabaseContainerStub jdbcContainer = new JdbcDatabaseContainerStub("mysql:latest") {
            @Override
            public String getDriverClassName() {
                return CapturingDriver.class.getName();
            }

            @Override
            public String getUsername() {
                return "test";
            }

            @Override
            public String getPassword() {
                return "test";
            }
        };

        Properties dataSourceProperties = new Properties();
        dataSourceProperties.setProperty("rewriteBatchedStatements", "true");
        dataSourceProperties.setProperty("profileSQL", "true");

        jdbcContainer.createConnection("", dataSourceProperties);

        // Raw Hashtable-style access (what many real JDBC drivers use) must see the values -
        // not just Properties#getProperty(), which would also find them via the old defaults-chain bug.
        assertThat(CapturingDriver.lastReceivedProperties.get("rewriteBatchedStatements")).isEqualTo("true");
        assertThat(CapturingDriver.lastReceivedProperties.get("profileSQL")).isEqualTo("true");
        assertThat(CapturingDriver.lastReceivedProperties.keySet())
            .contains("rewriteBatchedStatements", "profileSQL", "user", "password");
    }

    /**
     * A minimal fake {@link Driver} that just records the {@link Properties} it was asked to connect with,
     * so tests can assert on raw Hashtable-style access rather than the defaults-aware {@code getProperty()}.
     */
    public static class CapturingDriver implements Driver {

        static volatile Properties lastReceivedProperties;

        @Override
        public Connection connect(String url, Properties info) {
            lastReceivedProperties = info;
            return mock(Connection.class);
        }

        @Override
        public boolean acceptsURL(String url) {
            return true;
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
            return new DriverPropertyInfo[0];
        }

        @Override
        public int getMajorVersion() {
            return 1;
        }

        @Override
        public int getMinorVersion() {
            return 0;
        }

        @Override
        public boolean jdbcCompliant() {
            return false;
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
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

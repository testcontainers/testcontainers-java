package org.testcontainers.containers;

import lombok.NonNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JdbcDatabaseContainerCreateConnectionTest {

    @Test
    void createConnectionMergesCallerInfoIntoDriverProperties() throws Exception {
        Driver driver = mock(Driver.class);
        Connection connection = mock(Connection.class);
        when(driver.connect(anyString(), any(Properties.class))).thenAnswer(invocation -> {
            Properties p = invocation.getArgument(1);
            assertThat(
                p.entrySet()
                    .stream()
                    .map(e -> (String) e.getKey())
                    .collect(Collectors.toSet())
            ).contains("rewriteBatchedStatements");
            assertThat(p.getProperty("rewriteBatchedStatements")).isEqualTo("true");
            assertThat(p.getProperty("user")).isEqualTo("u");
            assertThat(p.getProperty("password")).isEqualTo("secret");
            return connection;
        });

        TestContainer container = new TestContainer(driver);
        Properties info = new Properties();
        info.setProperty("rewriteBatchedStatements", "true");

        assertThat(container.createConnection("", info)).isSameAs(connection);
    }

    static class TestContainer extends JdbcDatabaseContainer<TestContainer> {

        private final Driver testDriver;

        TestContainer(Driver testDriver) {
            super(DockerImageName.parse("mysql:5.7"));
            this.testDriver = testDriver;
        }

        @Override
        public String getDriverClassName() {
            return "ignored";
        }

        @Override
        public String getJdbcUrl() {
            return "jdbc:mock://localhost/db";
        }

        @Override
        public String getUsername() {
            return "u";
        }

        @Override
        public String getPassword() {
            return "secret";
        }

        @Override
        protected String getTestQueryString() {
            return "SELECT 1";
        }

        @Override
        public Driver getJdbcDriverInstance() {
            return testDriver;
        }

        @Override
        public boolean isRunning() {
            return true;
        }

        @Override
        protected Logger logger() {
            return mock(Logger.class);
        }

        @Override
        public void setDockerImageName(@NonNull String dockerImageName) {}
    }
}

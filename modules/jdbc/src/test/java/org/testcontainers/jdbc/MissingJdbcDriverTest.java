package org.testcontainers.jdbc;

import com.google.common.base.Throwables;
import org.junit.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.rnorth.visibleassertions.VisibleAssertions.fail;

public class MissingJdbcDriverTest {

    @Test
    public void shouldFailFastIfNoDriverFound() {

        final MissingDriverContainer container = new MissingDriverContainer();

        try {
            container.start();
            fail("The container is expected to fail to start");
        } catch (Exception e) {
            final Throwable rootCause = Throwables.getRootCause(e);
            assertTrue("ClassNotFoundException is the root cause", rootCause instanceof ClassNotFoundException);
        } finally {
            container.stop();
        }

        assertEquals("only one connection attempt should have been made", 1, container.getConnectionAttempts());
    }

    /**
     * Container class for the purposes of testing, with a known non-existent driver
     */
    static class MissingDriverContainer extends JdbcDatabaseContainer {
        private final AtomicInteger connectionAttempts = new AtomicInteger();

        MissingDriverContainer() {
            super(DockerImageName.parse("mysql:5.7.34"));
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

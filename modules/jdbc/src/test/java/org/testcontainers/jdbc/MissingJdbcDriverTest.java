package org.testcontainers.jdbc;

import com.google.common.base.Throwables;
import org.junit.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.rnorth.visibleassertions.VisibleAssertions.fail;

public class MissingJdbcDriverTest {

    @Test
    public void shouldFailFastIfNoDriverFound() {

        AtomicInteger connectionAttempts = new AtomicInteger();

        // Anonymous inner class for the purposes of testing, with a known non-existent driver testFailFastIfNoDriverFound
        final JdbcDatabaseContainer container = new JdbcDatabaseContainer("mysql:5.7.22") {

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
                return "";
            }

            @Override
            public String getPassword() {
                return "";
            }

            @Override
            protected String getTestQueryString() {
                return "";
            }

            @Override
            public Connection createConnection(String queryString) throws SQLException, NoDriverFoundException {
                connectionAttempts.incrementAndGet(); // test window: so we know how many times a connection was attempted
                return super.createConnection(queryString);
            }
        };

        try {
            container.start();
            fail("The container is expected to fail to start");
        } catch (Exception e) {
            final Throwable rootCause = Throwables.getRootCause(e);
            assertTrue("ClassNotFoundException is the root cause", rootCause instanceof ClassNotFoundException);
        } finally {
            container.stop();
        }

        assertEquals("only one connection attempt should have been made", 1, connectionAttempts.get());
    }
}

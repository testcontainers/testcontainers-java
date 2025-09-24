package org.testcontainers.jdbc.pgvector;

import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

class PgVectorJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                {
                    "jdbc:tc:pgvector://hostname/databasename?user=someuser&password=somepwd",
                    EnumSet.of(Options.JDBCParams),
                },
                {
                    "jdbc:tc:pgvector:pg14://hostname/databasename?user=someuser&password=somepwd",
                    EnumSet.of(Options.JDBCParams),
                },
            }
        );
    }
}

package org.testcontainers.jdbc.pgvector;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

@RunWith(Parameterized.class)
public class PgVectorJDBCDriverTest extends AbstractJDBCDriverTest {

    @Parameterized.Parameters(name = "{index} - {0}")
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

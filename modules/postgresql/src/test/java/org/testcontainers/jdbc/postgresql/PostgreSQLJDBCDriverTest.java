package org.testcontainers.jdbc.postgresql;

import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

public class PostgreSQLJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                {
                    "jdbc:tc:postgresql:9.6.8://hostname/databasename?user=someuser&password=somepwd",
                    EnumSet.of(Options.JDBCParams),
                },
            }
        );
    }
}

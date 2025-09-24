package org.testcontainers.jdbc.yugabytedb;

import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * YugabyteDB YSQL API JDBC connectivity driver test class
 */
class YugabyteDBYSQLJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                {
                    "jdbc:tc:yugabyte://hostname/yugabyte?user=yugabyte&password=yugabyte",
                    EnumSet.noneOf(Options.class),
                },
            }
        );
    }
}

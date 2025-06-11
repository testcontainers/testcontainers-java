package org.testcontainers.jdbc.yugabytedb;

import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * YugabyteDB YSQL API JDBC connectivity driver test class
 */
@ParameterizedClass
@MethodSource("data")
public class YugabyteDBYSQLJDBCDriverTest extends AbstractJDBCDriverTest {

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

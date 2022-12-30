package org.testcontainers.jdbc.yugabytedb;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * YugabyteDB YSQL API JDBC connectivity driver test class
 *
 * @author srinivasa-vasu
 */
@RunWith(Parameterized.class)
public class YugabyteDBYSQLJDBCDriverTest extends AbstractJDBCDriverTest {

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                {
                    "jdbc:tc:yugabytedb://hostname/yugabyte?user=yugabyte&password=yugabyte",
                    EnumSet.noneOf(Options.class),
                },
            }
        );
    }
}

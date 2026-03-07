package org.testcontainers.oceanbase;

import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

class OceanBaseJdbcDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] { { "jdbc:tc:oceanbasece://hostname/databasename", EnumSet.noneOf(Options.class) } }
        );
    }
}

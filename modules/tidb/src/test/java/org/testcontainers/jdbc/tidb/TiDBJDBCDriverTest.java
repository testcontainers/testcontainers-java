package org.testcontainers.jdbc.tidb;

import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

public class TiDBJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] { { "jdbc:tc:tidb://hostname/databasename", EnumSet.noneOf(Options.class) } }
        );
    }
}

package org.testcontainers.doris;

import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

class DorisJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] { { "jdbc:tc:doris:3.1.0://hostname/databasename", EnumSet.noneOf(Options.class) } }
        );
    }
}

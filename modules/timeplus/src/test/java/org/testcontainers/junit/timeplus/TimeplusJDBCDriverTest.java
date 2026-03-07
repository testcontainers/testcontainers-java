package org.testcontainers.junit.timeplus;

import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

class TimeplusJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] { { "jdbc:tc:timeplus:2.3.21://hostname", EnumSet.noneOf(Options.class) } }
        );
    }
}

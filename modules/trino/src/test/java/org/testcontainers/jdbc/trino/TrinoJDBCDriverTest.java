package org.testcontainers.jdbc.trino;

import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

class TrinoJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] { //
                { "jdbc:tc:trino:352://hostname/", EnumSet.of(Options.PmdKnownBroken) },
            }
        );
    }
}

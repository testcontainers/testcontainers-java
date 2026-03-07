package org.testcontainers.jdbc.presto;

import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

class PrestoJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] { //
                { "jdbc:tc:presto:344://hostname/", EnumSet.of(Options.PmdKnownBroken) },
            }
        );
    }
}

package org.testcontainers.databend;

import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

class DatabendJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] { //
                { "jdbc:tc:databend://hostname/databasename", EnumSet.of(Options.PmdKnownBroken) },
            }
        );
    }
}

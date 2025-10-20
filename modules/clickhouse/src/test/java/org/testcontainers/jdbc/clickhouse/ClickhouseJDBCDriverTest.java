package org.testcontainers.jdbc.clickhouse;

import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

class ClickhouseJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] { //
                { "jdbc:tc:clickhouse://hostname/databasename", EnumSet.of(Options.PmdKnownBroken) },
            }
        );
    }
}

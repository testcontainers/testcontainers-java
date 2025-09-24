package org.testcontainers.jdbc.questdb;

import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

class QuestDBJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                { "jdbc:tc:postgresql://hostname/databasename", EnumSet.of(Options.PmdKnownBroken) },
                { "jdbc:tc:questdb://hostname/databasename", EnumSet.of(Options.PmdKnownBroken) },
            }
        );
    }
}

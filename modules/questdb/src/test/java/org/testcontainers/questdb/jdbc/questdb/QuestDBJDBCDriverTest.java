package org.testcontainers.questdb.jdbc.questdb;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.jdbctest.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

@RunWith(Parameterized.class)
public class QuestDBJDBCDriverTest extends AbstractJDBCDriverTest {

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] { { "jdbc:tc:postgresql://hostname/databasename", EnumSet.of(Options.PmdKnownBroken) } }
        );
    }
}

package org.testcontainers.jdbc.timescaledb;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.EnumSet;

import static java.util.Arrays.asList;

@RunWith(Parameterized.class)
public class TimescaleDBJDBCDriverTest extends AbstractJDBCDriverTest {

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Iterable<Object[]> data() {
        return asList(
            new Object[][]{
                {"jdbc:tc:timescaledb://hostname/databasename?user=someuser&password=somepwd", EnumSet.of(Options.JDBCParams)},
                {"jdbc:tc:timescaledb:2.1.0-pg13://hostname/databasename?user=someuser&password=somepwd", EnumSet.of(Options.JDBCParams)},
            });
    }
}

package org.testcontainers.jdbc.postgresql;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.EnumSet;

import static java.util.Arrays.asList;

@RunWith(Parameterized.class)
public class PostgreSQLJDBCDriverTest extends AbstractJDBCDriverTest {

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Iterable<Object[]> data() {
        return asList(
            new Object[][]{
                {"jdbc:tc:postgresql:9.6.8://hostname/databasename?user=someuser&password=somepwd", EnumSet.of(Options.JDBCParams)},
            });
    }
}

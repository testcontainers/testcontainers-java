package org.testcontainers.db2.jdbc.db2;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.jdbctest.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

@RunWith(Parameterized.class)
public class DB2JDBCDriverTest extends AbstractJDBCDriverTest {

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] { //
                { "jdbc:tc:db2://hostname/databasename", EnumSet.noneOf(Options.class) },
            }
        );
    }
}

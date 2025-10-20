package org.testcontainers.jdbc.db2;

import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

class DB2JDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] { //
                { "jdbc:tc:db2://hostname/databasename", EnumSet.noneOf(Options.class) },
            }
        );
    }
}

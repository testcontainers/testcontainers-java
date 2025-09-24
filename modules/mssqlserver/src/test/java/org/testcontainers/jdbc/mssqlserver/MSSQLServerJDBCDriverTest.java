package org.testcontainers.jdbc.mssqlserver;

import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

class MSSQLServerJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                {
                    "jdbc:tc:sqlserver:2022-CU14-ubuntu-22.04://hostname:hostport;databaseName=databasename",
                    EnumSet.noneOf(Options.class),
                },
            }
        );
    }
}

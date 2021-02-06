package org.testcontainers.jdbc.postgis;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.EnumSet;

import static java.util.Arrays.asList;

@RunWith(Parameterized.class)
public class PostgisJDBCDriverTest extends AbstractJDBCDriverTest {

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Iterable<Object[]> data() {
        return asList(
            new Object[][]{
                {"jdbc:tc:postgis://hostname/databasename?user=someuser&password=somepwd", EnumSet.of(Options.JDBCParams)},
                {"jdbc:tc:postgis:9.6-2.5://hostname/databasename?user=someuser&password=somepwd", EnumSet.of(Options.JDBCParams)},
            });
    }
}

package org.testcontainers.jdbc.postgis;

import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

@ParameterizedClass
@MethodSource("data")
public class PostgisJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                {
                    "jdbc:tc:postgis://hostname/databasename?user=someuser&password=somepwd",
                    EnumSet.of(Options.JDBCParams),
                },
                {
                    "jdbc:tc:postgis:9.6-2.5://hostname/databasename?user=someuser&password=somepwd",
                    EnumSet.of(Options.JDBCParams),
                },
            }
        );
    }
}

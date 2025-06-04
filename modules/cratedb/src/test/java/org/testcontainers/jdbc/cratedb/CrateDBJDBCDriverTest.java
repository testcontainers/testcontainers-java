package org.testcontainers.jdbc.cratedb;

import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

@ParameterizedClass
@MethodSource("data")
public class CrateDBJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                { "jdbc:tc:cratedb:5.2.3://hostname/crate?user=crate&password=somepwd", EnumSet.noneOf(Options.class) },
            }
        );
    }
}

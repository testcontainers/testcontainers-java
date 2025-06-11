package org.testcontainers.jdbc.cockroachdb;

import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

@ParameterizedClass
@MethodSource("data")
public class CockroachDBJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] { //
                { "jdbc:tc:cockroach:v21.2.17://hostname/databasename", EnumSet.noneOf(Options.class) },
            }
        );
    }
}

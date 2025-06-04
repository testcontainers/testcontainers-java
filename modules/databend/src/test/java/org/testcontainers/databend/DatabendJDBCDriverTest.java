package org.testcontainers.databend;

import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runner.RunWith;
import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

@ParameterizedClass
@MethodSource("data")
public class DatabendJDBCDriverTest extends AbstractJDBCDriverTest {

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] { //
                { "jdbc:tc:databend://hostname/databasename", EnumSet.of(Options.PmdKnownBroken) },
            }
        );
    }
}

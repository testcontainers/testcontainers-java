package org.testcontainers.jdbc.gaussdb;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.jdbc.AbstractJDBCDriverTest;

import java.util.Arrays;
import java.util.EnumSet;

@RunWith(Parameterized.class)
public class GaussDBJDBCDriverTest extends AbstractJDBCDriverTest {

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                {
                    "jdbc:tc:gaussdb:///postgres?user=gaussdb&password=Enmo@123",
                    EnumSet.of(Options.JDBCParams),
                },
            }
        );
    }
}

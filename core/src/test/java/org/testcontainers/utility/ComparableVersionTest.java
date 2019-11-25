package org.testcontainers.utility;

import java.util.Arrays;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertArrayEquals;

@RunWith(Parameterized.class)
public class ComparableVersionTest {

    private final int[] expected;
    private final String given;

    public ComparableVersionTest(final String given, final int[] expected) {
        this.given = given;
        this.expected = expected;
    }

    @Test
    public void shouldParseVersions() {
        assertArrayEquals(expected, ComparableVersion.parseVersion(given));
    }

    @Parameters(name = "Parsed version: {0}={1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
            {"1.2.3", new int[] {1, 2, 3}},
            {"", new int[0]},
            {"1", new int[] {1}},
            {"1.2.3.4.5.6.7", new int[] {1, 2, 3, 4, 5, 6, 7}},
            {"1.2-dev", new int[] {1, 2}},
            {"18.06.0-dev", new int[] {18, 6}},
        });
    }

}

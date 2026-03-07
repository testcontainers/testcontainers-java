package org.testcontainers.utility;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class ComparableVersionTest {

    @ParameterizedTest(name = "Parsed version: {0}={1}")
    @MethodSource("data")
    void shouldParseVersions(String given, int[] expected) {
        assertThat(ComparableVersion.parseVersion(given)).containsExactly(expected);
    }

    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                { "1.2.3", new int[] { 1, 2, 3 } },
                { "", new int[0] },
                { "1", new int[] { 1 } },
                { "1.2.3.4.5.6.7", new int[] { 1, 2, 3, 4, 5, 6, 7 } },
                { "1.2-dev", new int[] { 1, 2 } },
                { "18.06.0-dev", new int[] { 18, 6 } },
            }
        );
    }
}

package org.testcontainers.utility;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.runners.Parameterized.Parameters;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class ComparableVersionTest {

    @ParameterizedTest(name = "Parsed version: {0}={1}")
    @MethodSource("provideParameters")
    public void shouldParseVersions(String given, final int[] expected) {
        assertThat(ComparableVersion.parseVersion(given)).containsExactly(expected);
    }

    @Parameters(name = "Parsed version: {0}={1}")
    public static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of("1.2.3", new int[] { 1, 2, 3 }),
            Arguments.of("", new int[0]),
            Arguments.of("1", new int[] { 1 }),
            Arguments.of("1.2.3.4.5.6.7", new int[] { 1, 2, 3, 4, 5, 6, 7 }),
            Arguments.of("1.2-dev", new int[] { 1, 2 }),
            Arguments.of("18.06.0-dev", new int[] { 18, 6 })
        );
    }
}

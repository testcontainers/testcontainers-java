package org.testcontainers.mssqlserver;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.MSSQLServerTestImages;
import org.testcontainers.containers.MSSQLServerContainer;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.fail;

/**
 * Tests if the password passed to the container satisfied the password policy described at
 * https://docs.microsoft.com/en-us/sql/relational-databases/security/password-policy?view=sql-server-2017
 */
public class CustomPasswordMSSQLServerTest {

    private static String UPPER_CASE_LETTERS = "ABCDE";

    private static String LOWER_CASE_LETTERS = "abcde";

    private static String NUMBERS = "12345";

    private static String SPECIAL_CHARS = "_(!)_";

    public static Stream<Arguments> data() {
        return Stream.of(
            Arguments.arguments(null, false),
            // too short
            Arguments.arguments("abc123", false),
            // too long
            Arguments.arguments(RandomStringUtils.randomAlphabetic(129), false),
            // only 2 categories
            Arguments.arguments(UPPER_CASE_LETTERS + NUMBERS, false),
            Arguments.arguments(UPPER_CASE_LETTERS + SPECIAL_CHARS, false),
            Arguments.arguments(LOWER_CASE_LETTERS + NUMBERS, false),
            Arguments.arguments(LOWER_CASE_LETTERS + SPECIAL_CHARS, false),
            Arguments.arguments(NUMBERS + SPECIAL_CHARS, false),
            // 3 categories
            Arguments.arguments(UPPER_CASE_LETTERS + LOWER_CASE_LETTERS + NUMBERS, true),
            Arguments.arguments(UPPER_CASE_LETTERS + LOWER_CASE_LETTERS + SPECIAL_CHARS, true),
            Arguments.arguments(UPPER_CASE_LETTERS + NUMBERS + SPECIAL_CHARS, true),
            Arguments.arguments(LOWER_CASE_LETTERS + NUMBERS + SPECIAL_CHARS, true),
            // 4 categories
            Arguments.arguments(UPPER_CASE_LETTERS + LOWER_CASE_LETTERS + NUMBERS + SPECIAL_CHARS, true)
        );
    }

    @ParameterizedTest
    @MethodSource("data")
    public void runPasswordTests(String password, boolean valid) {
        try {
            new MSSQLServerContainer<>(MSSQLServerTestImages.MSSQL_SERVER_IMAGE).withPassword(password);
            if (!valid) {
                fail("Password " + password + " is not valid. Expected exception");
            }
        } catch (IllegalArgumentException e) {
            if (valid) {
                fail("Password " + password + " should have been validated");
            }
        }
    }
}

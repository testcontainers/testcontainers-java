package org.testcontainers.junit.mssqlserver;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.MSSQLServerTestImages;
import org.testcontainers.containers.MSSQLServerContainer;

import java.util.Arrays;
import java.util.Collection;

import static org.rnorth.visibleassertions.VisibleAssertions.fail;

/**
 * Tests if the password passed to the container satisfied the password policy described at
 * https://docs.microsoft.com/en-us/sql/relational-databases/security/password-policy?view=sql-server-2017
 *
 * @author Enrico Costanzi
 */
@RunWith(Parameterized.class)
public class CustomPasswordMSSQLServerTest {

    private static String UPPER_CASE_LETTERS = "ABCDE";
    private static String LOWER_CASE_LETTERS = "abcde";
    private static String NUMBERS = "12345";
    private static String SPECIAL_CHARS = "_(!)_";

    private String password;
    private Boolean valid;

    public CustomPasswordMSSQLServerTest(String password, Boolean valid) {
        this.password = password;
        this.valid = valid;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            new Object[]{null, false},
            // too short
            {"abc123", false},

            // too long
            {RandomStringUtils.randomAlphabetic(129), false},

            // only 2 categories
            {UPPER_CASE_LETTERS + NUMBERS, false},
            {UPPER_CASE_LETTERS + SPECIAL_CHARS, false},
            {LOWER_CASE_LETTERS + NUMBERS, false},
            {LOWER_CASE_LETTERS + SPECIAL_CHARS, false},
            {NUMBERS + SPECIAL_CHARS, false},

            // 3 categories
            {UPPER_CASE_LETTERS + LOWER_CASE_LETTERS + NUMBERS, true},
            {UPPER_CASE_LETTERS + LOWER_CASE_LETTERS + SPECIAL_CHARS, true},
            {UPPER_CASE_LETTERS + NUMBERS + SPECIAL_CHARS, true},
            {LOWER_CASE_LETTERS + NUMBERS + SPECIAL_CHARS, true},

            // 4 categories
            {UPPER_CASE_LETTERS + LOWER_CASE_LETTERS + NUMBERS + SPECIAL_CHARS, true},


        });
    }

    @Test
    public void runPasswordTests() {
        try {
            new MSSQLServerContainer<>(MSSQLServerTestImages.MSSQL_SERVER_IMAGE).withPassword(this.password);
            if (!valid)
                fail("Password " + this.password + " is not valid. Expected exception");
        } catch (IllegalArgumentException e) {
            if (valid)
                fail("Password " + this.password + " should have been validated");
        }
    }


}

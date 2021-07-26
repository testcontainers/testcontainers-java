package org.testcontainers.testsupport;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FlakyRuleTest {

    private static final String VALID_URL = "http://some.url/here";
    private static final String INVALID_URL = "";

    private static final String VALID_DATE_IN_FAR_FUTURE = "2063-04-05";
    private static final String VALID_DATE_IN_PAST = "1991-08-16";
    private static final String INVALID_DATE = "91-01-45";
    private static final int DEFAULT_TRIES = 3;

    private FlakyTestJUnit4RetryRule rule = new FlakyTestJUnit4RetryRule();

    @Test
    public void testIgnoresMethodWithoutAnnotation() throws Throwable {
        final Description description = newDescriptionWithoutAnnotation();
        final DummyStatement statement = newStatement(3);
        try {
            rule.apply(statement, description).evaluate();
            fail("Should not reach here");
        } catch (Exception ignored) {

        }
        assertEquals("The statement should only be invoked once, even if it throws", 1, statement.invocationCount);
    }

    @Test
    public void testRetriesMethodWithAnnotationUntilFailure() throws Throwable {
        final Description description = newDescriptionWithAnnotation();

        final DummyStatement statement = newStatement(3);
        try {
            rule.apply(statement, description).evaluate();
            fail("Should not reach here");
        } catch (Exception ignored) {

        }
        assertEquals("The statement should be invoked three times", 3, statement.invocationCount);
    }

    @Test
    public void testCustomRetryCount() throws Throwable {
        final Description description = newDescriptionWithAnnotationAndCustomTries(10);

        final DummyStatement statement = newStatement(10);
        try {
            rule.apply(statement, description).evaluate();
            fail("Should not reach here");
        } catch (Exception ignored) {

        }
        assertEquals("The statement should be invoked ten times", 10, statement.invocationCount);
    }

    @Test
    public void testRetriesMethodWithAnnotationUntilSuccess() throws Throwable {
        final Description description = newDescriptionWithAnnotation();

        final DummyStatement statement = newStatement(2);

        rule.apply(statement, description).evaluate();

        assertEquals("The statement should be invoked three times, and succeed the third time", 3, statement.invocationCount);
    }

    @Test
    public void testDoesNotRetryMethodWithAnnotationIfNotThrowing() throws Throwable {
        final Description description = newDescriptionWithAnnotation();

        final DummyStatement statement = newStatement(0);

        rule.apply(statement, description).evaluate();

        assertEquals("The statement should be invoked once", 1, statement.invocationCount);
    }

    @Test
    public void testTreatsExpiredAnnotationAsNoAnnotation() throws Throwable {
        final Description description = newDescriptionWithExpiredAnnotation();
        final DummyStatement statement = newStatement(3);
        try {
            rule.apply(statement, description).evaluate();
            fail("Should not reach here");
        } catch (Exception ignored) {

        }
        assertEquals("The statement should only be invoked once, even if it throws", 1, statement.invocationCount);
    }

    @Test
    public void testThrowsOnInvalidDateFormat() throws Throwable {
        final Description description = newDescriptionWithAnnotation(INVALID_DATE, VALID_URL);
        final DummyStatement statement = newStatement(3);
        try {
            rule.apply(statement, description).evaluate();
            fail("Should not reach here");
        } catch (IllegalArgumentException ignored) {

        }
        assertEquals("The statement should not be invoked if the annotation is invalid", 0, statement.invocationCount);
    }

    @Test
    public void testThrowsOnInvalidGitHubUrl() throws Throwable {
        final Description description = newDescriptionWithAnnotation(VALID_DATE_IN_FAR_FUTURE, INVALID_URL);
        final DummyStatement statement = newStatement(3);
        try {
            rule.apply(statement, description).evaluate();
            fail("Should not reach here");
        } catch (IllegalArgumentException ignored) {

        }
        assertEquals("The statement should not be invoked if the annotation is invalid", 0, statement.invocationCount);
    }

    private Description newDescriptionWithAnnotation(String reviewDate, String gitHubUrl) {
        return Description.createTestDescription("SomeTestClass", "someMethod", newAnnotation(reviewDate, gitHubUrl, DEFAULT_TRIES));
    }

    private Description newDescriptionWithoutAnnotation() {
        return Description.createTestDescription("SomeTestClass", "someMethod");
    }

    private Description newDescriptionWithAnnotation() {
        return Description.createTestDescription("SomeTestClass", "someMethod", newAnnotation(VALID_DATE_IN_FAR_FUTURE, VALID_URL, DEFAULT_TRIES));
    }

    private Description newDescriptionWithAnnotationAndCustomTries(int maxTries) {
        return Description.createTestDescription("SomeTestClass", "someMethod", newAnnotation(VALID_DATE_IN_FAR_FUTURE, VALID_URL, maxTries));
    }

    private Description newDescriptionWithExpiredAnnotation() {
        return Description.createTestDescription("SomeTestClass", "someMethod", newAnnotation(VALID_DATE_IN_PAST, VALID_URL, DEFAULT_TRIES));
    }

    private Flaky newAnnotation(final String reviewDate, String gitHubUrl, int tries) {
        return new Flaky() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Flaky.class;
            }

            @Override
            public String githubIssueUrl() {
                return gitHubUrl;
            }

            @Override
            public String reviewDate() {
                return reviewDate;
            }

            @Override
            public int maxTries() {
                return tries;
            }
        };
    }

    private DummyStatement newStatement(int timesToThrow) {
        final DummyStatement statement = new DummyStatement();
        statement.shouldThrowTimes = timesToThrow;
        return statement;
    }

    private static class DummyStatement extends Statement {
        int shouldThrowTimes = -1;
        int invocationCount = 0;

        @Override
        public void evaluate() {
            invocationCount++;

            if (shouldThrowTimes > 0) {
                shouldThrowTimes--;
                throw new RuntimeException();
            }
        }
    }
}

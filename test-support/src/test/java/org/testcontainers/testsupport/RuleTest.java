package org.testcontainers.testsupport;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RuleTest {

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

    private Description newDescriptionWithoutAnnotation() {
        return Description.createTestDescription("SomeTestClass", "someMethod");
    }

    private Description newDescriptionWithAnnotation() {
        return Description.createTestDescription("SomeTestClass", "someMethod", newAnnotation("2063-04-05"));
    }

    private Description newDescriptionWithExpiredAnnotation() {
        return Description.createTestDescription("SomeTestClass", "someMethod", newAnnotation("1991-08-16"));
    }

    private Flaky newAnnotation(final String reviewDate) {
        return new Flaky() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return Flaky.class;
            }

            @Override
            public String rationale() {
                return "";
            }

            @Override
            public String reviewDate() {
                return reviewDate;
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

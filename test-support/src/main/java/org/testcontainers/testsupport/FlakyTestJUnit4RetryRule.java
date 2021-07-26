package org.testcontainers.testsupport;

import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     JUnit 4 @Rule that implements retry for flaky tests (tests that suffer from sporadic random failures).
 * </p>
 * <p>
 *     This rule should be used in conjunction with the @{@link Flaky} annotation. When this Rule is applied to a test
 *     class, any test method with this annotation will be invoked up to 3 times or until it succeeds.
 * </p>
 * <p>
 *     Tests should <em>not</em> be marked @{@link Flaky} for a long period of time. Every usage should be
 *     accompanied by a GitHub issue URL, and should be subject to review at a suitable point in the (near) future.
 *     Should the review date pass without the test's instability being fixed, the retry behaviour will cease to have an
 *     effect and the test will be allowed to sporadically fail again.
 * </p>
 */
@Slf4j
public class FlakyTestJUnit4RetryRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {

        final Flaky annotation = description.getAnnotation(Flaky.class);

        if (annotation == null) {
            // leave the statement as-is
            return base;
        }

        if (annotation.githubIssueUrl().trim().length() == 0) {
            throw new IllegalArgumentException("A GitHub issue URL must be set for usages of the @Flaky annotation");
        }

        final int maxTries = annotation.maxTries();

        if (maxTries < 1) {
            throw new IllegalArgumentException("@Flaky annotation maxTries must be at least one");
        }

        final LocalDate reviewDate;
        try {
            reviewDate = LocalDate.parse(annotation.reviewDate());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("@Flaky reviewDate could not be parsed. Please provide a date in yyyy-mm-dd format");
        }

        // the annotation should only have an effect before the review date, to encourage review and resolution
        if ( LocalDate.now().isBefore(reviewDate) ) {
            return new RetryingStatement(base, description, maxTries);
        } else {
            return base;
        }
    }

    private static class RetryingStatement extends Statement {
        private final Statement base;
        private final Description description;
        private final int maxTries;

        RetryingStatement(Statement base, Description description, int maxTries) {
            this.base = base;
            this.description = description;
            this.maxTries = maxTries;
        }

        @Override
        public void evaluate() {

            int attempts = 0;
            final List<Throwable> causes = new ArrayList<>();

            while (++attempts <= maxTries) {
                try {
                    base.evaluate();
                    return;
                } catch (Throwable throwable) {
                    log.warn("Retrying @Flaky-annotated test: {}", description.getDisplayName());
                    causes.add(throwable);
                }
            }

            throw new IllegalStateException(
                "@Flaky-annotated test failed despite retries.",
                new MultipleFailureException(causes));
        }
    }
}

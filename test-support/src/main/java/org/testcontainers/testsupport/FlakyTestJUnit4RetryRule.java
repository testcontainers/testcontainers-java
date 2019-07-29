package org.testcontainers.testsupport;

import lombok.extern.slf4j.Slf4j;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO: Javadocs
 */
@Slf4j
public class FlakyTestJUnit4RetryRule implements TestRule {

    @Override
    public Statement apply(Statement base, Description description) {
        final Flaky annotation = description.getAnnotation(Flaky.class);
        if (annotation != null) {
            if ( ! isReviewDatePassed(annotation)) {
                return new RetryingStatement(base, description);
            }
        }

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                base.evaluate();
            }
        };
    }

    private boolean isReviewDatePassed(Flaky annotation) {
        final LocalDate reviewDate = LocalDate.parse(annotation.reviewDate());

        return reviewDate.isBefore(LocalDate.now());
    }

    private static class RetryingStatement extends Statement {
        private final Statement base;
        private final Description description;

        RetryingStatement(Statement base, Description description) {
            this.base = base;
            this.description = description;
        }

        @Override
        public void evaluate() {

            int attempts = 0;
            final List<Throwable> causes = new ArrayList<>();

            while (++attempts <= 3) {
                try {
                    base.evaluate();
                    return;
                } catch (Throwable throwable) {
                    log.info("Retrying @Flaky-annotated test: {}", description.getDisplayName());
                    causes.add(throwable);
                }
            }

            throw new IllegalStateException(
                "@Flaky-annotated test failed despite retries.",
                new MultipleFailureException(causes));
        }
    }
}

package org.testcontainers.junit.vintage;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.testcontainers.lifecycle.TestDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link TestRule} which is called before and after each test, and also is notified on success/failure.
 *
 * This mimics the behaviour of TestWatcher to some degree, but failures occurring in {@code starting()}
 * prevent the test from being run.
 */
class FailureDetectingExternalResource implements TestRule {

    @Override
    public final Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                List<Throwable> errors = new ArrayList<Throwable>();
                Optional<Throwable> failure = Optional.empty();

                try {
                    starting(description);
                    base.evaluate();
                    notifySucceeded(description, errors);
                } catch (org.junit.internal.AssumptionViolatedException e) {
                    failure = Optional.of(e);
                } catch (Throwable e) {
                    failure = Optional.of(e);
                    errors.add(e);
                    notifyFailed(e, description);
                } finally {
                    notifyFinished(failure, description, errors);
                }

                MultipleFailureException.assertEmpty(errors);
            }
        };
    }

    protected void starting(Description description) throws Throwable {}

    protected void succeeded(Description description) throws Throwable {}

    protected void failed(Throwable e, Description description) throws Throwable {}

    protected void finished(Description description) throws Throwable {}

    private void notifySucceeded(Description description, List<Throwable> errors) {
        try {
            succeeded(description);
        } catch (Throwable e) {
            errors.add(e);
        }
    }

    private void notifyFailed(Throwable failure, Description description) {
        try {
            failed(failure, description);
        } catch (Throwable e) {
            failure.addSuppressed(e);
        }
    }

    private void notifyFinished(Optional<Throwable> failure, Description description, List<Throwable> errors) {
        try {
            finished(description);
        } catch (Throwable e) {
            failure.ifPresent(f -> f.addSuppressed(e)); // ifPresentOrElse() requires Java 9
            if (!failure.isPresent()) {
                errors.add(e);
            }
        }
    }

    protected static final TestDescription toTestDescription(Description description) {
        return new TestDescription() {
            @Override
            public String getTestId() {
                return description.getDisplayName();
            }

            @Override
            public String getFilesystemFriendlyName() {
                return description.getClassName() + "-" + description.getMethodName();
            }
        };
    }
}

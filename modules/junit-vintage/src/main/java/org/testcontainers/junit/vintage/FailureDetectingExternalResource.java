package org.testcontainers.junit.vintage;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.testcontainers.lifecycle.TestDescription;

import java.util.ArrayList;
import java.util.List;

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

                try {
                    starting(description);
                    base.evaluate();
                    notifySucceeded(description, errors);
                } catch (org.junit.internal.AssumptionViolatedException e) {
                    // Do nothing.
                } catch (Throwable e) {
                    errors.add(e);
                    notifyFailed(e, description, errors);
                } finally {
                    notifyFinished(description, errors);
                }

                MultipleFailureException.assertEmpty(errors);
            }
        };
    }

    protected void starting(Description description) throws Throwable {}

    protected void succeeded(Description description) throws Throwable {}

    protected void failed(Throwable e, Description description) {}

    protected void finished(Description description, List<Throwable> errors) {}

    private void notifySucceeded(Description description, List<Throwable> errors) {
        try {
            succeeded(description);
        } catch (Throwable e) {
            errors.add(e);
        }
    }

    private void notifyFailed(Throwable failure, Description description, List<Throwable> errors) {
        try {
            failed(failure, description);
        } catch (Throwable e) {
            errors.add(e);
        }
    }

    private void notifyFinished(Description description, List<Throwable> errors) {
        try {
            finished(description, errors);
        } catch (Throwable e) {
            errors.add(e);
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

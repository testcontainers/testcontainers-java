package org.testcontainers.containers;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TestRule} which is called before and after each test, and also is notified on success/failure.
 *
 * This mimics the behaviour of TestWatcher to some degree, but failures occurring in this rule do not
 * contribute to the overall failure count (which can otherwise cause strange negative test success
 * figures).
 */
public class FailureDetectingExternalResource implements TestRule {
    @Override
    public Statement apply(Statement base, Description description) {

        return new Statement() {
            @Override
            public void evaluate() throws Throwable {

                List<Throwable> errors = new ArrayList<Throwable>();

                try {
                    starting(description);
                    base.evaluate();
                    succeeded(description);
                } catch (Throwable e) {
                    errors.add(e);
                    failed(e, description);
                } finally {
                    finished(description);
                }

                MultipleFailureException.assertEmpty(errors);
            }
        };
    }

    protected void starting(Description description) {

    }

    protected void succeeded(Description description) {
    }

    protected void failed(Throwable e, Description description) {
    }

    protected void finished(Description description) {
    }
}

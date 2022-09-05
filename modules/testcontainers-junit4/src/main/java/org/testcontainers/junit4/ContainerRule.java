package org.testcontainers.junit4;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContainerRule<T extends Startable> implements TestRule {

    private final T container;

    public ContainerRule(T container) {
        this.container = container;
    }

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

    private TestDescription toDescription(Description description) {
        return new TestDescription() {
            @Override
            public String getTestId() {
                return description.getDisplayName();
            }

            @Override
            public String getFilesystemFriendlyName() {
                return description.getClassName() + "" + description.getMethodName();
            }
        };
    }

    protected void starting(Description description) {
        if (container instanceof TestLifecycleAware) {
            final TestLifecycleAware lifecycleAware = (TestLifecycleAware) container;
            lifecycleAware.beforeTest(toDescription(description));
        }
        container.start();
    }

    protected void succeeded(Description description) {
        if (container instanceof TestLifecycleAware) {
            final TestLifecycleAware lifecycleAware = (TestLifecycleAware) container;
            lifecycleAware.afterTest(toDescription(description), Optional.empty());
        }
    }

    protected void failed(Throwable e, Description description) {
        if (container instanceof TestLifecycleAware) {
            final TestLifecycleAware lifecycleAware = (TestLifecycleAware) container;
            lifecycleAware.afterTest(toDescription(description), Optional.of(e));
        }
    }

    protected void finished(Description description) {
        container.stop();
    }

    public T get() {
        return container;
    }
}

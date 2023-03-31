package org.testcontainers;

import org.jetbrains.annotations.NotNull;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.containers.FailureDetectingExternalResource;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.util.Optional;

public class TestcontainersRule extends FailureDetectingExternalResource {

    private final Startable container;

    public TestcontainersRule(Startable container) {
        this.container = container;
    }

    @NotNull
    @Override
    public Statement apply(Statement base, Description description) {
        return super.apply(base, description);
    }

    protected void starting(Description description) {
        if (this instanceof TestLifecycleAware) {
            ((TestLifecycleAware) this).beforeTest(toDescription(description));
        }
        this.container.start();
    }

    protected void succeeded(Description description) {
        if (this instanceof TestLifecycleAware) {
            ((TestLifecycleAware) this).afterTest(toDescription(description), Optional.empty());
        }
    }

    protected void failed(Throwable e, Description description) {
        if (this instanceof TestLifecycleAware) {
            ((TestLifecycleAware) this).afterTest(toDescription(description), Optional.of(e));
        }
    }

    protected void finished(Description description) {
        this.container.stop();
    }

    private TestDescription toDescription(Description description) {
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

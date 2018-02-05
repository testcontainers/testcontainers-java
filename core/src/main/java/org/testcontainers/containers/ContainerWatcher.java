package org.testcontainers.containers;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ContainerWatcher extends TestWatcher {

    private final GenericContainer watchedContainer;

    public ContainerWatcher(GenericContainer container) {
        this.watchedContainer = container;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        watchedContainer.apply();
        return super.apply(base, description);
    }

    @Override
    protected void succeeded(Description description) {
        watchedContainer.succeeded(description);
        super.succeeded(description);
    }

    @Override
    protected void failed(Throwable e, Description description) {
        watchedContainer.failed(e, description);
        super.failed(e, description);
    }

}

package org.testcontainers.junit.vintage;

import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TestLifecycleAwareContainerMock implements Startable, TestLifecycleAware {

    static final String START = "start";

    static final String BEFORE_TEST = "beforeTest";

    static final String AFTER_TEST = "afterTest";

    static final String STOP = "stop";

    private final List<String> lifecycleMethodCalls = new ArrayList<>();

    private final List<String> lifecycleFilesystemFriendlyNames = new ArrayList<>();

    private Throwable capturedThrowable;

    @Override
    public void beforeTest(TestDescription description) {
        lifecycleMethodCalls.add(BEFORE_TEST);
        lifecycleFilesystemFriendlyNames.add(description.getFilesystemFriendlyName());
    }

    @Override
    public void afterTest(TestDescription description, Optional<Throwable> throwable) {
        lifecycleMethodCalls.add(AFTER_TEST);
        throwable.ifPresent(capturedThrowable -> this.capturedThrowable = capturedThrowable);
    }

    List<String> getLifecycleMethodCalls() {
        return lifecycleMethodCalls;
    }

    Throwable getCapturedThrowable() {
        return capturedThrowable;
    }

    public List<String> getLifecycleFilesystemFriendlyNames() {
        return lifecycleFilesystemFriendlyNames;
    }

    @Override
    public void start() {
        lifecycleMethodCalls.add(START);
    }

    @Override
    public void stop() {
        lifecycleMethodCalls.add(STOP);
    }
}

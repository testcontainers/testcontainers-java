package org.testcontainers.junit.jupiter;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TestLifecycleAwareContainerMock extends GenericContainer implements TestLifecycleAware {

    static final String BEFORE_TEST = "beforeTest";
    static final String AFTER_TEST = "afterTest";

    private List<String> lifecycleMethodCalls = new ArrayList<>();

    private Throwable capturedThrowable;

    TestLifecycleAwareContainerMock() {
        super("alpine:3.2");
        setCommand("top");
    }

    @Override
    public void beforeTest(TestDescription description) {
        lifecycleMethodCalls.add(BEFORE_TEST);
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
}

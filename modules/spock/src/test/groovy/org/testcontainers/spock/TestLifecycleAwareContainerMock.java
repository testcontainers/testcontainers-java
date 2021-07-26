package org.testcontainers.spock;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.testcontainers.spock.SpockTestImages.TINY_IMAGE;

public class TestLifecycleAwareContainerMock extends GenericContainer<TestLifecycleAwareContainerMock> implements TestLifecycleAware {

    static final String BEFORE_TEST = "beforeTest";
    static final String AFTER_TEST = "afterTest";

    final List<String> lifecycleMethodCalls = new ArrayList<>();
    final List<String> lifecycleFilesystemFriendlyNames = new ArrayList<>();

    Throwable capturedThrowable;

    public TestLifecycleAwareContainerMock() {
        super(TINY_IMAGE);
    }

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

    @Override
    public void start() {
        // Do nothing
    }

    @Override
    public void stop() {
        // Do nothing
    }
}

package org.testcontainers.junit.jupiter;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.util.Optional;

public class TestLifecycleAwareContainerMock extends GenericContainer implements TestLifecycleAware {

    private int numBeforeTestsCalls = 0;
    private int numAfterTestsCalls = 0;

    TestLifecycleAwareContainerMock() {
        super("alpine:3.2");
        setCommand("top");
    }

    @Override
    public void beforeTest(TestDescription description) {
        numBeforeTestsCalls++;
    }

    @Override
    public void afterTest(TestDescription description, Optional<Throwable> throwable) {
        numAfterTestsCalls++;
    }

    int getNumBeforeTestsCalls() {
        return numBeforeTestsCalls;
    }

    int getNumAfterTestsCalls() {
        return numAfterTestsCalls;
    }
}

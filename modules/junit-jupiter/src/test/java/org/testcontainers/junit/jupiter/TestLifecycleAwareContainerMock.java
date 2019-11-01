package org.testcontainers.junit.jupiter;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.util.Optional;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;

public class TestLifecycleAwareContainerMock extends GenericContainer implements TestLifecycleAware {

    private int numBeforeTestsCalls = 0;
    private int numAfterTestsCalls = 0;

    private long beforeTestCalledAtMillis = 0;
    private long afterTestCalledAtMillis = 0;

    private Throwable capturedThrowable;

    TestLifecycleAwareContainerMock() {
        super("alpine:3.2");
        setCommand("top");
    }

    @Override
    public void beforeTest(TestDescription description) {
        numBeforeTestsCalls++;
        beforeTestCalledAtMillis = currentTimeMillis();
        ensureMillisHavePassed();
    }

    @Override
    public void afterTest(TestDescription description, Optional<Throwable> throwable) {
        numAfterTestsCalls++;
        afterTestCalledAtMillis = currentTimeMillis();
        throwable.ifPresent(capturedThrowable -> this.capturedThrowable = capturedThrowable);
    }

    private void ensureMillisHavePassed() {
        try {
            sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to sleep for 10ms");
        }
    }

    int getNumBeforeTestsCalls() {
        return numBeforeTestsCalls;
    }

    int getNumAfterTestsCalls() {
        return numAfterTestsCalls;
    }

    long getBeforeTestCalledAtMillis() {
        return beforeTestCalledAtMillis;
    }

    long getAfterTestCalledAtMillis() {
        return afterTestCalledAtMillis;
    }

    Throwable getCapturedThrowable() {
        return capturedThrowable;
    }
}

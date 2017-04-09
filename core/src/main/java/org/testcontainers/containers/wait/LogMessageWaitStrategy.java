package org.testcontainers.containers.wait;

import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * Waits until containers logs expected content.
 */
public class LogMessageWaitStrategy extends GenericContainer.AbstractWaitStrategy {
    private String regEx;

    private int times = 1;

    @Override
    protected void waitUntilReady() {
        WaitingConsumer waitingConsumer = new WaitingConsumer();
        container.followOutput(waitingConsumer);

        Predicate<OutputFrame> waitPredicate = outputFrame ->
                outputFrame.getUtf8String().matches(regEx);

        try {
            waitingConsumer.waitUntil(waitPredicate, startupTimeout.getSeconds(), TimeUnit.SECONDS, times);
        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for log output matching '" + regEx + "'");
        }
    }

    public LogMessageWaitStrategy withRegEx(String regEx) {
        this.regEx = regEx;
        return this;
    }

    public LogMessageWaitStrategy withTimes(int times) {
        this.times = times;
        return this;
    }
}

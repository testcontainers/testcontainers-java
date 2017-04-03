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
    private String expectedLogPart;

    @Override
    protected void waitUntilReady() {
        WaitingConsumer waitingConsumer = new WaitingConsumer();
        container.followOutput(waitingConsumer);

        Predicate<OutputFrame> waitPredicate = outputFrame ->
                outputFrame.getUtf8String().contains(expectedLogPart);

        try {
            waitingConsumer.waitUntil(waitPredicate, startupTimeout.getSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for log output containing '" + expectedLogPart + "'");
        }
    }

    public LogMessageWaitStrategy withExpectedLogPart(String expectedLogPart) {
        this.expectedLogPart = expectedLogPart;
        return this;
    }
}

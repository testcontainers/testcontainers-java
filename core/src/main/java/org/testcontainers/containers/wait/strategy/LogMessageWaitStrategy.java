package org.testcontainers.containers.wait.strategy;

import com.github.dockerjava.api.command.LogContainerCmd;
import lombok.SneakyThrows;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public class LogMessageWaitStrategy extends AbstractLogMessageWaitStrategy {

    private String regEx;

    @Override
    protected Predicate<OutputFrame> waitPredicate() {
        return outputFrame -> {
            // (?s) enables line terminator matching (equivalent to Pattern.DOTALL)
            return outputFrame.getUtf8String().matches("(?s)" + regEx);
        };
    }

    @Override
    protected String timeoutErrorMessage() {
        return "Timed out waiting for log output matching '" + regEx + "'";
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

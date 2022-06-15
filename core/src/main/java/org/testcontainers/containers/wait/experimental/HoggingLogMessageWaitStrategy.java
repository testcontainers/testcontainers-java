package org.testcontainers.containers.wait.experimental;

import com.github.dockerjava.api.command.LogContainerCmd;
import lombok.SneakyThrows;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/*
    This wait strategy has following characteristics
    - Validates the regex which has been configured with the entire log history. It does this by using a StringBuffer (thread safety) to concat everything together.
    - Logic for "x amount of times" or complex setups such as "(a OR b) AND c" is fully to be implemented by regex.
    - "times" is removed from the class interface.

    Risks:
    - Slower for extremely long logs lines.
 */

public class HoggingLogMessageWaitStrategy extends AbstractWaitStrategy {

    // Hardcoded, as we assume the regEx will check the amount of times for this implementation.
    private static final int CHECK_TIMES = 1;

    private String regEx;

    @Override
    @SneakyThrows(IOException.class)
    protected void waitUntilReady() {
        WaitingConsumer waitingConsumer = new WaitingConsumer();

        LogContainerCmd cmd = waitStrategyTarget
            .getDockerClient()
            .logContainerCmd(waitStrategyTarget.getContainerId())
            .withFollowStream(true)
            .withSince(0)
            .withStdOut(true)
            .withStdErr(true);

        try (FrameConsumerResultCallback callback = new FrameConsumerResultCallback()) {
            callback.addConsumer(OutputFrame.OutputType.STDOUT, waitingConsumer);
            callback.addConsumer(OutputFrame.OutputType.STDERR, waitingConsumer);

            cmd.exec(callback);

            //region Specific Implementation

            final StringBuffer builder = new StringBuffer();

            Predicate<OutputFrame> waitPredicate = outputFrame -> {
                builder.append(outputFrame.getUtf8String());
                // (?s) enables line terminator matching (equivalent to Pattern.DOTALL)
                return builder.toString().matches("(?s)" + regEx);
            };

            //endregion

            try {
                waitingConsumer.waitUntil(waitPredicate, startupTimeout.getSeconds(), TimeUnit.SECONDS, CHECK_TIMES);
            } catch (TimeoutException e) {
                throw new ContainerLaunchException("Timed out waiting for log output matching '" + regEx + "'");
            }
        }
    }

    public HoggingLogMessageWaitStrategy withRegEx(String regEx) {
        this.regEx = regEx;
        return this;
    }
}

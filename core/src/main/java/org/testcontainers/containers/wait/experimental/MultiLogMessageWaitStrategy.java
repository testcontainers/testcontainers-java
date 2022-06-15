package org.testcontainers.containers.wait.experimental;

import com.github.dockerjava.api.command.LogContainerCmd;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/*
    This wait strategy has following characteristics
    - Allows multiple regex to be added, which will be validated in order.
    - Logic for "x amount of times" or complex setups such as "(a OR b) AND c" is fully to be implemented by regex (multiple if needed).
    - "times" is removed from the class interface.
 */

@Slf4j
public class MultiLogMessageWaitStrategy extends AbstractWaitStrategy {

    private static final int CHECK_TIMES = 1;

    private LinkedBlockingDeque<String> regEx;

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

            Predicate<OutputFrame> waitPredicate = outputFrame -> {
                String nextExpectedRegex = regEx.peek();
                // (?s) enables line terminator matching (equivalent to Pattern.DOTALL)
                if(outputFrame.getUtf8String().matches("(?s)" + nextExpectedRegex)) {
                    // remove the matched item from the collection.
                    regEx.pop();
                    log.info("Regex {} encountered. Waiting for {} more regex statements.", nextExpectedRegex, regEx.size());
                }
                // If collection is now empty, we are finished.
                return regEx.isEmpty();
            };
            try {
                waitingConsumer.waitUntil(waitPredicate, startupTimeout.getSeconds(), TimeUnit.SECONDS, CHECK_TIMES);
            } catch (TimeoutException e) {
                throw new ContainerLaunchException("Timed out waiting for log output matching '" + regEx + "'");
            }
        }
    }

    public MultiLogMessageWaitStrategy withRegex(String... regEx) {
        // TODO, add validation that we have at least one regex. :-)
        this.regEx = new LinkedBlockingDeque<>(Arrays.asList(regEx));
        return this;
    }

}

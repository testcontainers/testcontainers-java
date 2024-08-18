package org.testcontainers.containers.wait.experimental;

import com.github.dockerjava.api.command.LogContainerCmd;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.AbstractLogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/*
    This wait strategy has following characteristics
    - Allows multiple regex to be added, which will be validated in order.
    - Logic for "x amount of times" or complex setups such as "(a OR b) AND c" is fully to be implemented by regex (multiple if needed).
    - "times" is removed from the class interface.
 */

@Slf4j
public class MultiLogMessageWaitStrategy extends AbstractLogMessageWaitStrategy {

    private LinkedBlockingDeque<String> regEx;

    @Override
    protected Predicate<OutputFrame> waitPredicate() {
        return outputFrame -> {
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
    }

    @Override
    protected String timeoutErrorMessage() {
        return "Timed out waiting for log output. Still expecting following log lines: '" + String.join(",", regEx) + "'";
    }

    public MultiLogMessageWaitStrategy withRegex(String... regEx) {
        this.regEx = new LinkedBlockingDeque<>(Arrays.asList(regEx));
        return this;
    }

}

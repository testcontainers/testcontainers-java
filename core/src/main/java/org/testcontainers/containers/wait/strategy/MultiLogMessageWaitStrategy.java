package org.testcontainers.containers.wait.strategy;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.utility.LogUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * This is a wait strategy to wait for multiple log patterns.
 * The wait strategy will continue when every log pattern is matched at least once.
 */
public class MultiLogMessageWaitStrategy extends AbstractWaitStrategy {

    private final ConcurrentHashMap<String, Boolean> regexes = new ConcurrentHashMap<>();

    @Override
    protected void waitUntilReady() {
        WaitingConsumer waitingConsumer = new WaitingConsumer();
        LogUtils.followOutput(DockerClientFactory.instance().client(), waitStrategyTarget.getContainerId(), waitingConsumer);

        Predicate<OutputFrame> waitPredicate = outputFrame -> {
            if (regexes.isEmpty()) {
                return true;
            }
            regexes.entrySet().forEach(regexHasMatched -> {
                final boolean matched = outputFrame.getUtf8String().matches("(?s)" + regexHasMatched.getKey());
                if (matched) {
                    regexHasMatched.setValue(true);
                }
            });
            return regexes.values().stream().reduce(Boolean::logicalAnd).orElse(true);
        };

        try {
            waitingConsumer.waitUntil(waitPredicate, startupTimeout.getSeconds(), TimeUnit.SECONDS, 1);
        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for log output matching '" + regexes + "'");
        }
    }

    public MultiLogMessageWaitStrategy withRegEx(final String regEx) {
        regexes.put(regEx, false);
        return this;
    }

    public MultiLogMessageWaitStrategy reset() {
        for (final Map.Entry<String, Boolean> stringBooleanEntry : regexes.entrySet()) {
            stringBooleanEntry.setValue(false);
        }
        return this;
    }
}

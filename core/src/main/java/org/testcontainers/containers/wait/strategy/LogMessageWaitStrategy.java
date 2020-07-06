package org.testcontainers.containers.wait.strategy;

import com.github.dockerjava.api.command.LogContainerCmd;
import lombok.SneakyThrows;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import static org.testcontainers.containers.output.OutputFrame.OutputType.STDERR;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

public class LogMessageWaitStrategy extends AbstractWaitStrategy {

    private String regEx;

    private int times = 1;

    @Override
    @SneakyThrows(IOException.class)
    protected void waitUntilReady() {
        WaitingConsumer waitingConsumer = new WaitingConsumer();

        LogContainerCmd cmd = DockerClientFactory.instance().client().logContainerCmd(waitStrategyTarget.getContainerId())
            .withFollowStream(true)
            .withSince(0)
            .withStdOut(true)
            .withStdErr(true);

        try (FrameConsumerResultCallback callback = new FrameConsumerResultCallback()) {
            callback.addConsumer(STDOUT, waitingConsumer);
            callback.addConsumer(STDERR, waitingConsumer);

            cmd.exec(callback);

            Predicate<OutputFrame> waitPredicate = outputFrame ->
                // (?s) enables line terminator matching (equivalent to Pattern.DOTALL)
                outputFrame.getUtf8String().matches("(?s)" + regEx);

            try {
                waitingConsumer.waitUntil(waitPredicate, startupTimeout.getSeconds(), TimeUnit.SECONDS, times);
            } catch (TimeoutException e) {
                throw new ContainerLaunchException("Timed out waiting for log output matching '" + regEx + "'");
            }
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

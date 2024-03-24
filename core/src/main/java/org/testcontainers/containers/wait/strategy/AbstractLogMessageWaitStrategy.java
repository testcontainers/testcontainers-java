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

public abstract class AbstractLogMessageWaitStrategy extends AbstractWaitStrategy {

    protected int times = 1;

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

            try {
                waitingConsumer.waitUntil(waitPredicate(), startupTimeout.getSeconds(), TimeUnit.SECONDS, times);
            } catch (TimeoutException e) {
                throw new ContainerLaunchException(timeoutErrorMessage());
            }
        }
    }

    protected abstract Predicate<OutputFrame> waitPredicate();
    protected abstract String timeoutErrorMessage();

}

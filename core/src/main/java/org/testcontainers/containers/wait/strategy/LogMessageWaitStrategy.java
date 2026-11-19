package org.testcontainers.containers.wait.strategy;

import com.github.dockerjava.api.command.LogContainerCmd;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.awaitility.core.TerminalFailureException;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

public class LogMessageWaitStrategy extends AbstractWaitStrategy {

    private String regEx;

    private int times = 1;

    @Override
    @SneakyThrows({ IOException.class, InterruptedException.class })
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
                // (?s) enables line terminator matching (equivalent to Pattern.DOTALL)
                return outputFrame.getUtf8String().matches("(?s)" + regEx);
            };

            // Run the log wait in the background, so that this thread can abort as soon as the
            // container exits, instead of waiting for the whole startup timeout.
            final Future<Boolean> logWaitFuture = EXECUTOR.submit(() -> {
                waitingConsumer.waitUntil(waitPredicate, startupTimeout.getSeconds(), TimeUnit.SECONDS, times);
                return true;
            });

            final Callable<Boolean> logWaitComplete = logWaitFuture::isDone;
            try {
                Awaitility
                    .await()
                    .pollInSameThread()
                    .pollInterval(Duration.ofMillis(100))
                    .pollDelay(Duration.ZERO)
                    .failFast("container is no longer running", () -> !waitStrategyTarget.isRunning())
                    .forever()
                    .until(logWaitComplete);
            } catch (TerminalFailureException e) {
                // The container exited while we were still waiting. Give the log wait a brief grace
                // period to match any output that was buffered right before the container exited,
                // before failing fast.
                try {
                    logWaitFuture.get(1, TimeUnit.SECONDS);
                    return;
                } catch (TimeoutException | ExecutionException | CancellationException graceException) {
                    throw new IllegalStateException(
                        "Container stopped before the expected log output matching '" + regEx + "' was produced"
                    );
                } finally {
                    logWaitFuture.cancel(true);
                }
            }

            try {
                logWaitFuture.get(0, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException | CancellationException e) {
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

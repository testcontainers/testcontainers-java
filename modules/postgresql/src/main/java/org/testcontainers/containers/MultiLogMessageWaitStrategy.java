package org.testcontainers.containers;

import com.github.dockerjava.api.command.LogContainerCmd;
import lombok.SneakyThrows;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import static org.testcontainers.containers.output.OutputFrame.OutputType.STDERR;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

public class MultiLogMessageWaitStrategy extends AbstractWaitStrategy {

    private List<List<String>> regExs = new ArrayList<>();

    private final int times = 1;

    private final long limit = 15;

    @Override
    @SneakyThrows(IOException.class)
    protected void waitUntilReady() {
        boolean success = true;
        for (List<String> ex : this.regExs) {
            WaitingConsumer waitingConsumer = new WaitingConsumer();
            try (FrameConsumerResultCallback callback = new FrameConsumerResultCallback()) {
                callback.addConsumer(STDOUT, waitingConsumer);
                callback.addConsumer(STDERR, waitingConsumer);
                success = true;

                try {
                    for (String regEx : ex) {
                        LogContainerCmd cmd = DockerClientFactory.instance().client().logContainerCmd(waitStrategyTarget.getContainerId())
                            .withFollowStream(true)
                            .withSince(0)
                            .withStdOut(true)
                            .withStdErr(true);
                        cmd.exec(callback);

                        Predicate<OutputFrame> waitPredicate = outputFrame ->
                            // (?s) enables line terminator matching (equivalent to Pattern.DOTALL)
                            outputFrame.getUtf8String().matches("(?s)" + regEx);

                        waitingConsumer.waitUntil(waitPredicate, limit, TimeUnit.SECONDS, times);
                    }
                } catch (TimeoutException e) {
                    success = false;
                }
                if (success) {
                    break;
                }
            }
        }
        if (!success) {
            throw new ContainerLaunchException("Timed out waiting for log output matching '" + "." + "'");
        }
    }

    public MultiLogMessageWaitStrategy withRegEx(List<String> regExs) {
        this.regExs.add(regExs);
        return this;
    }

}

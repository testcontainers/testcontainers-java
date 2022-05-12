package org.testcontainers.containers;

import com.github.dockerjava.api.command.LogContainerCmd;
import lombok.SneakyThrows;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.utility.ComparableVersion;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import static org.testcontainers.containers.output.OutputFrame.OutputType.STDERR;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

public class PostgreSQLWaitStrategy extends AbstractWaitStrategy {

    private final String version;

    public PostgreSQLWaitStrategy(String version) {
        this.version = version;
    }

    @Override
    @SneakyThrows(IOException.class)
    protected void waitUntilReady() {
        int times = 1;
        long limit = 15;
        boolean isAtLeastMajorVersion94 = new ComparableVersion(this.version).isGreaterThanOrEqualTo("9.4");

        List<List<String>> regExs = new ArrayList<>();

        List<String> first = new ArrayList<>();
        first.add(".*PostgreSQL init process complete.*$");
        first.add(".*database system is ready to accept connections.*$");
        regExs.add(first);

        List<String> second = new ArrayList<>();
        if (isAtLeastMajorVersion94) {
            second.add(".*PostgreSQL Database directory appears to contain a database.*$");
        }
        second.add(".*database system is ready to accept connections.*$");
        regExs.add(second);

        boolean success = true;
        for (List<String> ex : regExs) {
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

}

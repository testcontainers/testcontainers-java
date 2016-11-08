package org.testcontainers.utility;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import lombok.experimental.UtilityClass;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;

import java.util.function.Consumer;

import static org.testcontainers.containers.output.OutputFrame.OutputType.STDERR;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

/**
 * Provides utility methods for logging.
 */
@UtilityClass
public class LogUtils {
    /**
     * {@inheritDoc}
     */
    public void followOutput(DockerClient dockerClient, String containerId,
                             Consumer<OutputFrame> consumer, OutputFrame.OutputType... types) {

        final LogContainerCmd cmd = dockerClient.logContainerCmd(containerId)
                .withFollowStream(true);

        final FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        for (OutputFrame.OutputType type : types) {
            callback.addConsumer(type, consumer);
            if (type == STDOUT) cmd.withStdOut(true);
            if (type == STDERR) cmd.withStdErr(true);
        }

        cmd.exec(callback);
    }
}

package org.testcontainers.utility;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.AuthConfig;
import com.google.common.base.MoreObjects;
import lombok.experimental.UtilityClass;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;

import java.util.function.Consumer;

import static com.google.common.base.Strings.isNullOrEmpty;
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
                .withFollowStream(true)
                .withSince(0);

        final FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        for (OutputFrame.OutputType type : types) {
            callback.addConsumer(type, consumer);
            if (type == STDOUT) cmd.withStdOut(true);
            if (type == STDERR) cmd.withStdErr(true);
        }

        cmd.exec(callback);
    }

    public void followOutput(DockerClient dockerClient, String containerId, Consumer<OutputFrame> consumer) {
        followOutput(dockerClient, containerId, consumer, STDOUT, STDERR);
    }

}

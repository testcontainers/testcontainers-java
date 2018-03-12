package org.testcontainers.containers;

import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.utility.LogUtils;

import java.util.function.Consumer;

public interface LogFollower extends ContainerState {

    /**
     * Follow container output, sending each frame (usually, line) to a consumer. Stdout and stderr will be followed.
     *
     * @param consumer consumer that the frames should be sent to
     */
    default void followOutput(Consumer<OutputFrame> consumer) {
        this.followOutput(consumer, OutputFrame.OutputType.STDOUT, OutputFrame.OutputType.STDERR);
    }

    /**
     * Follow container output, sending each frame (usually, line) to a consumer. This method allows Stdout and/or stderr
     * to be selected.
     *
     * @param consumer consumer that the frames should be sent to
     * @param types    types that should be followed (one or both of STDOUT, STDERR)
     */
    default void followOutput(Consumer<OutputFrame> consumer, OutputFrame.OutputType... types) {
        LogUtils.followOutput(DockerClientFactory.instance().client(), getContainerId(), consumer, types);
    }
}

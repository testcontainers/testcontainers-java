package org.testcontainers.containers;

import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * A command that can be executed inside the {@link RabbitMQContainer}.
 */
public abstract class RabbitMQCommand {

    /**
     * Executes this command in the given container.
     *
     * <p>Errors are logged and suppressed.</p>
     *
     * <p>If this command has been added to a {@link RabbitMQContainer},
     * then the {@link RabbitMQContainer} will call this method when the container starts.</p>
     *
     * @param container the container in which to execute this command.
     */
    public void executeInContainer(RabbitMQContainer container) {
        List<String> cli = toCli();
        try {
            Container.ExecResult execResult = container.execInContainer(cli.toArray(new String[0]));
            if (execResult.getExitCode() != 0) {
                container.logger().error("Could not execute command {}: {}", String.join(" ", cli), execResult.getStderr());
            }
        } catch (IOException | InterruptedException e) {
            container.logger().error("Could not execute command {}: {}", String.join(" ", cli), e.getMessage());
        }
    }

    /**
     * The full command line to execute for this command.
     *
     * @return the full command line to execute for this command.
     */
    @NotNull
    protected abstract List<String> toCli();

}

package org.testcontainers.containers.rabbitmq.plugins;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.RabbitMQCommand;

/**
 * A {@link RabbitMQCommand} that executes the {@code rabbitmq-plugins} CLI command.
 */
public abstract class RabbitMQPluginsCommand extends RabbitMQCommand {

    private final String command;

    public RabbitMQPluginsCommand(String command) {
        this.command = Objects.requireNonNull(command, "command must not be null");
    }

    @Override
    @NotNull
    protected List<String> toCli() {
        return Stream.of(
                    Collections.singletonList("rabbitmq-plugins"),
                    Collections.singletonList(command),
                    getCliArguments())
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    @NotNull
    protected abstract List<String> getCliArguments();

}

package org.testcontainers.containers.rabbitmq.admin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * Declares a vhost limit.
 *
 * <p>The vhost defaults to the default vhost ({@value #DEFAULT_VHOST}).</p>
 */
public class DeclareVhostLimitCommand extends DeclareCommand<DeclareVhostLimitCommand> {

    public static final String MAX_CONNECTIONS = "max-connections";
    public static final String MAX_QUEUES = "max-queues";

    private final String name;

    private final int value;

    public DeclareVhostLimitCommand(String name, int value) {
        super("vhost_limit");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.value = value;
        // Set the default vhost so that user's don't need to explicitly configure it
        vhost(DEFAULT_VHOST);
    }

    @Override
    public DeclareVhostLimitCommand vhost(String vhost) {
        return super.vhost(Objects.requireNonNull(vhost, "vhost must not be null"));
    }

    @Override
    protected @NotNull List<String> getCliOptions() {
        // Do not include --vhost CLI option, since vhost CLI argument is used instead.
        return Collections.emptyList();
    }

    @Override
    @NotNull
    protected List<String> getCliArguments() {
        return Arrays.asList(
            cliArgument("vhost", getVhost()),
            cliArgument("name", name),
            cliArgument("value", Integer.toString(value)));
    }
}

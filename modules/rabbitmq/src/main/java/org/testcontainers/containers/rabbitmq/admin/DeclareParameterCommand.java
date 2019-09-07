package org.testcontainers.containers.rabbitmq.admin;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * Declares a parameter.
 */
public class DeclareParameterCommand extends DeclareCommand<DeclareParameterCommand> {

    private final String component;
    private final String name;
    private final String value;

    public DeclareParameterCommand(String component, String name, String value) {
        super("parameter");
        this.component = Objects.requireNonNull(component, "component must not be null");
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    @Override
    @NotNull
    protected List<String> getCliArguments() {
        return Arrays.asList(
            cliArgument("component", component),
            cliArgument("name", name),
            cliArgument("value", value));
    }
}

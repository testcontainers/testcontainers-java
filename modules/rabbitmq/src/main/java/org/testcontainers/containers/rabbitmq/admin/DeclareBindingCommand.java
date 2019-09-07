package org.testcontainers.containers.rabbitmq.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * Declares a queue binding.
 */
public class DeclareBindingCommand extends DeclareCommand<DeclareBindingCommand> {

    public static final String MATCH_ANY = "any";
    public static final String MATCH_ALL = "all";

    public static final String ARG_X_MATCH = "x-match";

    private final String source;

    private final String destination;

    private String routingKey;

    private String destinationType;

    private final Map<String, Object> arguments = new HashMap<>();

    /**
     * @param source source of the binding (e.g. the name of an exchange)
     * @param destination destination of the binding (e.g. the name of a queue)
     */
    public DeclareBindingCommand(String source, String destination) {
        super("binding");
        this.source = Objects.requireNonNull(source, "source must not be null");
        this.destination = Objects.requireNonNull(destination, "destination must not be null");
    }

    public DeclareBindingCommand routingKey(String routingKey) {
        this.routingKey = routingKey;
        return self();
    }

    public DeclareBindingCommand destinationType(String destinationType) {
        this.destinationType = destinationType;
        return self();
    }


    /**
     * Set the {@value #ARG_X_MATCH} argument, which is used when binding to a Headers Exchange.
     *
     * @param match the match value.  Usually either {@value #MATCH_ANY} or {@value #MATCH_ALL}.
     * @return this command
     */
    public DeclareBindingCommand match(String match) {
        return argument(ARG_X_MATCH, match);
    }

    /**
     * Sets the {@value #ARG_X_MATCH} argument to {@value #MATCH_ANY}.
     * @return this command
     */
    public DeclareBindingCommand matchAny() {
        return match(MATCH_ANY);
    }

    /**
     * Sets the {@value #ARG_X_MATCH} argument to {@value #MATCH_ALL}.
     * @return this command
     */
    public DeclareBindingCommand matchAll() {
        return match(MATCH_ALL);
    }

    /**
     * Sets the argument with the given name to the given value.
     * @param name name of the argument
     * @param value value of the argument
     * @return this command
     */
    public DeclareBindingCommand argument(String name, Object value) {
        this.arguments.put(name, value);
        verifyCanConvertToJson(this.arguments);
        return self();
    }

    /**
     * Sets all of the given arguments.
     * The given arguments are merged into any arguments that have previously been set.
     * @param arguments the arguments to set.
     * @return this command
     */
    public DeclareBindingCommand arguments(Map<String, Object> arguments) {
        this.arguments.putAll(arguments);
        verifyCanConvertToJson(this.arguments);
        return self();
    }

    @Override
    @NotNull
    protected List<String> getCliArguments() {

        List<String> cliArguments = new ArrayList<>();
        cliArguments.add(cliArgument("source", source));
        cliArguments.add(cliArgument("destination", destination));

        if (routingKey != null) {
            cliArguments.add(cliArgument("routing_key", routingKey));
        }

        if (destinationType != null) {
            cliArguments.add(cliArgument("destination_type", destinationType));
        }

        if (!arguments.isEmpty()) {
            cliArguments.add(cliArgument("arguments", arguments));
        }

        return cliArguments;
    }
}

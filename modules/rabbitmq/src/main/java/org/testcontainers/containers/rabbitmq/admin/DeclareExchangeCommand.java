package org.testcontainers.containers.rabbitmq.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * Declares a exchange.
 *
 * <p>Note the exchange type must be set after construction.</p>
 */
public class DeclareExchangeCommand extends DeclareCommand<DeclareExchangeCommand> {

    public static final String TYPE_DIRECT = "direct";
    public static final String TYPE_HEADERS = "headers";
    public static final String TYPE_FANOUT = "fanout";
    public static final String TYPE_TOPIC = "topic";

    public static final String ARG_ALTERNATE_EXCHANGE = "alternate-exchange";

    private final String name;

    private String type;

    private boolean autoDelete;

    private boolean internal;

    private boolean durable;

    private final Map<String, Object> arguments = new HashMap<>();

    public DeclareExchangeCommand(String name) {
        super("exchange");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Set the exchange type to {@value #TYPE_DIRECT}.
     * @return this command
     */
    public DeclareExchangeCommand direct() {
        return type(TYPE_DIRECT);
    }
    /**
     * Set the exchange type to {@value #TYPE_HEADERS}.
     * @return this command
     */
    public DeclareExchangeCommand headers() {
        return type(TYPE_HEADERS);
    }
    /**
     * Set the exchange type to {@value #TYPE_FANOUT}.
     * @return this command
     */
    public DeclareExchangeCommand fanout() {
        return type(TYPE_FANOUT);
    }
    /**
     * Set the exchange type to {@value #TYPE_TOPIC}.
     * @return this command
     */
    public DeclareExchangeCommand topic() {
        return type(TYPE_TOPIC);
    }

    /**
     * Set the exchange type.
     * @param type the exchange type
     * @return this command.
     */
    public DeclareExchangeCommand type(String type) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        return self();
    }

    /**
     * Set the exchange to auto delete.
     * @return this command
     */
    public DeclareExchangeCommand autoDelete() {
        return autoDelete(true);
    }

    /**
     * Set the exchange to auto delete or not.
     * @param autoDelete whether or not to auto delete
     * @return this command
     */
    public DeclareExchangeCommand autoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
        return self();
    }

    /**
     * Set the exchange to internal.
     * @return this command
     */
    public DeclareExchangeCommand internal() {
        return internal(true);
    }

    /**
     * Set the exchange to internal or not.
     * @param internal whether or not the exchange is internal
     * @return this command
     */
    public DeclareExchangeCommand internal(boolean internal) {
        this.internal = internal;
        return self();
    }

    /**
     * Set the exchange to durable.
     * @return this command
     */
    public DeclareExchangeCommand durable() {
        return durable(true);
    }

    /**
     * Set the exchange to durable or not
     * @param durable whether or not the exchange is durable
     * @return this command
     */
    public DeclareExchangeCommand durable(boolean durable) {
        this.durable = durable;
        return self();
    }

    /**
     * Sets the {@value #ARG_ALTERNATE_EXCHANGE} argument.
     *
     * @param alternateExchange the value of the {@value #ARG_ALTERNATE_EXCHANGE} argument
     * @return this command
     */
    public DeclareExchangeCommand alternateExchange(String alternateExchange) {
        return argument(ARG_ALTERNATE_EXCHANGE, alternateExchange);
    }

    /**
     * Sets the argument with the given name to the given value.
     * @param name name of the argument
     * @param value value of the argument
     * @return this command
     */
    public DeclareExchangeCommand argument(String name, Object value) {
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
    public DeclareExchangeCommand arguments(Map<String, Object> arguments) {
        this.arguments.putAll(arguments);
        verifyCanConvertToJson(this.arguments);
        return self();
    }

    @Override
    @NotNull
    protected List<String> getCliArguments() {

        List<String> cliArguments = new ArrayList<>();
        cliArguments.add(cliArgument("name", name));
        cliArguments.add(cliArgument("type", Objects.requireNonNull(type, () -> "type must be specified for " + getObjectType() +  " with name " + name)));

        if (autoDelete) {
            cliArguments.add(cliArgument("auto_delete", Boolean.toString(autoDelete)));
        }

        if (internal) {
            cliArguments.add(cliArgument("internal", Boolean.toString(internal)));
        }

        if (durable) {
            cliArguments.add(cliArgument("durable", Boolean.toString(durable)));
        }

        if (!arguments.isEmpty()) {
            cliArguments.add(cliArgument("arguments", arguments));
        }

        return cliArguments;
    }
}

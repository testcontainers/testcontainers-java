package org.testcontainers.containers.rabbitmq.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

/**
 * Declares a queue.
 */
public class DeclareQueueCommand extends DeclareCommand<DeclareQueueCommand> {

    public static final String ARG_X_DEAD_LETTER_EXCHANGE = "x-dead-letter-exchange";
    public static final String ARG_X_DEAD_LETTER_ROUTING_KEY = "x-dead-letter-routing-key";
    public static final String ARG_X_EXPIRES = "x-expires";
    public static final String ARG_X_MAX_LENGTH = "x-max-length";
    public static final String ARG_X_MAX_LENGTH_BYTES = "x-max-length-bytes";
    public static final String ARG_X_MAX_PRIORITY = "x-max-priority";
    public static final String ARG_X_MESSAGE_TTL = "x-message-ttl";
    public static final String ARG_X_OVERFLOW = "x-overflow";

    private final String name;

    private boolean autoDelete;

    private boolean durable;

    private final Map<String, Object> arguments = new HashMap<>();

    public DeclareQueueCommand(String name) {
        super("queue");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Sets the queue to auto delete.
     * @return this command
     */
    public DeclareQueueCommand autoDelete() {
        return autoDelete(true);
    }

    /**
     * Sets the queue to auto delete or not.
     * @param autoDelete whether or not to auto delete
     * @return this command
     */
    public DeclareQueueCommand autoDelete(boolean autoDelete) {
        this.autoDelete = autoDelete;
        return self();
    }

    /**
     * Set the queue to durable.
     * @return this command
     */
    public DeclareQueueCommand durable() {
        return durable(true);
    }

    /**
     * Set the queue to durable or not
     * @param durable whether or not the queue is durable
     * @return this command
     */
    public DeclareQueueCommand durable(boolean durable) {
        this.durable = durable;
        return self();
    }

    /**
     * Sets the {@value #ARG_X_DEAD_LETTER_EXCHANGE} argument.
     * @param deadLetterExchange the dead letter exchange
     * @return this command
     */
    public DeclareQueueCommand deadLetterExchange(String deadLetterExchange) {
        return argument(ARG_X_DEAD_LETTER_EXCHANGE, deadLetterExchange);
    }

    /**
     * Sets the {@value #ARG_X_DEAD_LETTER_ROUTING_KEY} argument.
     *
     * @param deadLetterRoutingKey the dead letter routing key
     * @return this command
     */
    public DeclareQueueCommand deadLetterRoutingKey(String deadLetterRoutingKey) {
        return argument(ARG_X_DEAD_LETTER_ROUTING_KEY, deadLetterRoutingKey);
    }

    /**
     * Sets the {@value #ARG_X_EXPIRES} argument.
     *
     * @param queueExpiresInMilliseconds queue expiration in milliseconds
     * @return this command
     */
    public DeclareQueueCommand expires(long queueExpiresInMilliseconds) {
        return argument(ARG_X_EXPIRES, queueExpiresInMilliseconds);
    }

    /**
     * Sets the {@value #ARG_X_MAX_LENGTH} argument.
     *
     * @param maxLength the max number of messages allowed in the queue
     * @return this command
     */
    public DeclareQueueCommand maxLength(long maxLength) {
        return argument(ARG_X_MAX_LENGTH, maxLength);
    }

    /**
     * Sets the {@value #ARG_X_MAX_LENGTH_BYTES} argument.
     *
     * @param maxLengthBytes the max length of the queue in bytes
     * @return this command
     */
    public DeclareQueueCommand maxLengthBytes(long maxLengthBytes) {
        return argument(ARG_X_MAX_LENGTH_BYTES, maxLengthBytes);
    }

    /**
     * Sets the {@value #ARG_X_MAX_PRIORITY} argument.
     * @param maxPriority the max priority
     * @return this command
     */
    public DeclareQueueCommand maxPriority(int maxPriority) {
        return argument(ARG_X_MAX_PRIORITY, maxPriority);
    }

    /**
     * Sets the {@value #ARG_X_MESSAGE_TTL} argument.
     * @param messageTtlInMilliseconds the message time to live in milliseconds
     * @return this command
     */
    public DeclareQueueCommand messageTtl(long messageTtlInMilliseconds) {
        return argument(ARG_X_MESSAGE_TTL, messageTtlInMilliseconds);
    }

    /**
     * Sets the {@value #ARG_X_OVERFLOW} argument.
     * @param overflow the overflow value
     * @return this command
     */
    public DeclareQueueCommand overflow(String overflow) {
        return argument(ARG_X_OVERFLOW, overflow);
    }

    /**
     * Sets the argument with the given name to the given value.
     * @param name name of the argument
     * @param value value of the argument
     * @return this command
     */
    public DeclareQueueCommand argument(String name, Object value) {
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
    public DeclareQueueCommand arguments(Map<String, Object> arguments) {
        this.arguments.putAll(arguments);
        verifyCanConvertToJson(this.arguments);
        return self();
    }

    @Override
    @NotNull
    protected List<String> getCliArguments() {

        List<String> cliArguments = new ArrayList<>();
        cliArguments.add(cliArgument("name", name));

        if (autoDelete) {
            cliArguments.add(cliArgument("auto_delete", Boolean.toString(autoDelete)));
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

package org.testcontainers.containers.rabbitmq.admin;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Declares an object on the RabbitMQ server when the container starts.
 *
 * <p>Concrete instances are generally created via the static factory
 * methods in {@link DeclareCommands}.</p>
 *
 * <p>For example, queues are declared by a {@link DeclareQueueCommand}
 * created via {@link DeclareCommands#queue(String)}.</p>
 *
 * @param <SELF> the type of command (used for the return type of fluent setter methods)
 */
public abstract class DeclareCommand<SELF extends DeclareCommand> extends RabbitMQAdminCommand<SELF> {

    /**
     * The type of object to declare. (e.g. queue)
     */
    private final String objectType;

    public DeclareCommand(String objectType) {
        this.objectType= Objects.requireNonNull(objectType, "objectType must not be null");
    }

    protected String getObjectType() {
        return objectType;
    }

    @Override
    protected List<String> getCliSubCommand() {
        return Arrays.asList("declare", objectType);
    }
}

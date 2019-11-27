package org.testcontainers.containers.rabbitmq.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Declares a vhost.
 *
 * <p>Objects within a vhost can be declared via {@link #declare(DeclareCommand)}.</p>
 */
public class DeclareVhostCommand extends DeclareCommand<DeclareVhostCommand> {

    private final String name;

    private boolean tracing;

    /**
     * Commands for declaring objects <em>within this vhost</em>.
     */
    private final List<DeclareCommand<? extends DeclareCommand>> commands = new ArrayList<>();

    public DeclareVhostCommand(String name) {
        super("vhost");
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Enables tracing.
     * @return this command
     */
    public DeclareVhostCommand tracing() {
        return tracing(true);
    }

    /**
     * Enables/disables tracing.
     * @param tracing true to enable tracing, false to disable
     * @return this command
     */
    public DeclareVhostCommand tracing(boolean tracing) {
        this.tracing = tracing;
        return self();
    }

    /**
     * Adds the given {@link DeclareCommand} to the list of commands that will execute
     * when the RabbitMQ container starts.
     *
     * <p>The {@link DeclareCommand} will declare an object on the RabbitMQ server
     * <em>in this vhost</em> when the container starts.</p>
     *
     * <p>The given {@link DeclareCommand}'s vhost will be automatically set to this vhost
     * via {@link DeclareCommand#vhost(String)}</p>
     *
     * <p>Use the static factory methods from {@link DeclareCommands} to create the {@code declareCommand}.</p>
     *
     * <p>For example:</p>
     * <pre>
     *     import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.queue;
     *     import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.vhost;
     *
     *     container.declare(
     *         vhost("myVhost")
     *             .declare(queue("myQueue").autoDelete()));
     * </pre>
     *
     * @param declareCommand declares a RabbitMQ object when the container starts
     * @return this container
     * @throws IllegalArgumentException if the given {@code declareCommand} is a {@link DeclareVhostCommand},
     *     since a vhost cannot be declared within another vhost.
     */
    public <T extends DeclareCommand<T>> DeclareVhostCommand declare(DeclareCommand<T> declareCommand) {
        if (declareCommand instanceof DeclareVhostCommand) {
            throw new IllegalArgumentException(String.format("Cannot declare vhost %s within vhost %s",
                ((DeclareVhostCommand) declareCommand).name,
                name));
        }
        commands.add(declareCommand.vhost(name));
        return self();
    }

    @Override
    public void executeInContainer(RabbitMQContainer container) {
        super.executeInContainer(container);
        commands.forEach(command -> command.vhost(name).executeInContainer(container));
    }

    @Override
    @NotNull
    protected List<String> getCliArguments() {

        List<String> cliArguments = new ArrayList<>();
        cliArguments.add(cliArgument("name", name));

        if (tracing) {
            cliArguments.add(cliArgument("tracing", Boolean.toString(tracing)));
        }

        return cliArguments;
    }
}

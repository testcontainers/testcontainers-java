package org.testcontainers.containers.rabbitmq.admin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.containers.RabbitMQCommand;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A {@link RabbitMQCommand} that executes the {@code rabbitmqadmin} CLI command.
 *
 * @param <SELF> the type of command (used for the return type of fluent setter methods)
 */
public abstract class RabbitMQAdminCommand<SELF extends RabbitMQAdminCommand> extends RabbitMQCommand {

    public static final String DEFAULT_VHOST = "/";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * The vhost name to pass to the {@code rabbitmqadmin} command.
     *
     * <p>Typically, this is used as a {@code --vhost=vhostName} option,
     * but some commands use this as a {@code vhost=vhostName} argument.</p>
     */
    @Nullable
    private String vhost;

    public String getVhost() {
        return vhost;
    }

    public SELF vhost(String vhost) {
        this.vhost = vhost;
        return self();
    }

    protected SELF self() {
        return (SELF) this;
    }

    @Override
    @NotNull
    protected List<String> toCli() {
        return Stream.of(
                Collections.singletonList("rabbitmqadmin"),
                getCliOptions(),
                getCliSubCommand(),
                getCliArguments())
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    /**
     * Return the CLI options (e.g. those that begin with --) for the command.
     * @return the CLI options (e.g. those that begin with --) for the command.
     */
    @NotNull
    protected List<String> getCliOptions() {
        if (vhost != null) {
            /*
             * Add the vhost option if it is specified.
             *
             * Note that some subclasses override this behavior,
             * and return the vhost as an argument in getCliArguments, instead of an option.
             */
            return Collections.singletonList(cliOption("vhost", vhost));
        }
        return Collections.emptyList();
    }

    /**
     * Return the subcommand (e.g. {@code declare queue}) for the command.
     * @return the subcommand (e.g. {@code declare queue}) for the command.
     */
    @NotNull
    protected abstract List<String> getCliSubCommand();

    /**
     * Return the arguments for the command (e.g. {@code name=value} pairs)
     * @return the arguments for the command (e.g. {@code name=value} pairs)
     */
    @NotNull
    protected abstract List<String> getCliArguments();

    /**
     * Return a CLI option in the form {@code --name=value}.
     * @param name the name of the option
     * @param value the value of the option
     * @return a CLI option in the form {@code --name=value}.
     */
    @NotNull
    protected String cliOption(String name, String value) {
        return "--" + name + "=" + value;
    }

    /**
     * Return a CLI argument in the form {@code name=value}.
     * @param name the name of the argument
     * @param value the value of the argument
     * @return a CLI argument in the form {@code name=value}.
     */
    @NotNull
    protected String cliArgument(String name, String value) {
        return name + "=" + value;
    }

    /**
     * Return a CLI argument in the form {@code name=jsonValue},
     * where {@code jsonValue} is the json serialized version of the given value map.
     *
     * @param name the name of the argument
     * @param value the map to serialize as the json value of the argument
     * @return a CLI argument in the form {@code name=jsonValue},
     *         where {@code jsonValue} is the json serialized version of the given value map.
     */
    @NotNull
    protected String cliArgument(String name, Map<String, Object> value) {
        return cliArgument(name, toJson(value));
    }

    /**
     * Throws an exception if the given map cannot be converted to JSON.
     * @param map a map to attempt to convert to JSON.
     */
    protected void verifyCanConvertToJson(Map<String, Object> map) {
        toJson(map);
    }

    /**
     * Converts the given map to a JSON string.
     * @param map the map to convert to a JSON string
     * @return the JSON serialized version of the given map
     * @throws IllegalArgumentException if the given map cannot be converted to JSON
     */
    @NotNull
    private String toJson(Map<String, Object> map) {
        try {
            return OBJECT_MAPPER.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert map into json: " + e.getMessage(), e);
        }
    }


}

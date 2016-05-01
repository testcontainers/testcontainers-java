package org.testcontainers.containers.traits;

import com.github.dockerjava.api.command.CreateContainerCmd;
import lombok.*;
import org.testcontainers.containers.Container;
import org.testcontainers.utility.SelfReference;

@Data
@AllArgsConstructor
public class Command<SELF extends Container<SELF>> implements Trait<SELF> {

    /**
     * This trait extension is required for backward compatibility
     *
     * @param <SELF>
     */
    public interface Support<SELF extends Container<SELF>> extends SelfReference<SELF> {

        /**
         * Set the command that should be run in the container. Consider using {@link #withCommand(String)}
         * for building a container in a fluent style.
         *
         * @param command a command in single string format (will automatically be split on spaces)
         */
        default void setCommand(@NonNull String command) {
            setCommand(command.split(" "));
        }

        /**
         * Set the command that should be run in the container. Consider using {@link #withCommand(String...)}
         * for building a container in a fluent style.
         *
         * @param commandParts a command as an array of string parts
         */
        default void setCommand(@NonNull String... commandParts) {
            Command command = self().computeTraitIfAbsent(Command.class, traitClass -> new Command<>(null));

            command.setCommandParts(commandParts);
        }

        /**
         * Set the command that should be run in the container
         *
         * @param cmd a command in single string format (will automatically be split on spaces)
         * @return this
         */
        default SELF withCommand(String cmd) {
            setCommand(cmd);
            return self();
        }

        /**
         * Set the command that should be run in the container
         *
         * @param commandParts a command as an array of string parts
         * @return this
         */
        default SELF withCommand(String... commandParts) {
            setCommand(commandParts);

            return self();
        }

        default String[] getCommandParts() {
            return self().getTrait(Command.class).map(Command::getCommandParts).orElse(null);
        }

        default void setCommandParts(String[] commandParts) {
            setCommand(commandParts);
        }
    }

    protected String[] commandParts = null;

    @Override
    public void configure(SELF container, CreateContainerCmd createContainerCmd) {
        if (commandParts != null) {
            createContainerCmd.withCmd(commandParts);
        }
    }
}

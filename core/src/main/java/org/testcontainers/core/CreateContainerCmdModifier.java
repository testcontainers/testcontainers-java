package org.testcontainers.core;

import com.github.dockerjava.api.command.CreateContainerCmd;

import java.util.function.Function;

/**
 * Callback interface that can be used to customize a {@link CreateContainerCmd}.
 */
public interface CreateContainerCmdModifier {
    /**
     * Callback to modify a {@link CreateContainerCmd} instance.
     */
    Function<CreateContainerCmd, CreateContainerCmd> modify();
}

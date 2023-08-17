package org.testcontainers.core;

import com.github.dockerjava.api.command.CreateContainerCmd;

/**
 * Callback interface that can be used to customize a {@link CreateContainerCmd}.
 */
public interface CreateContainerCmdCustomizer {
    /**
     * Callback to customize a {@link CreateContainerCmd} instance.
     * @param createContainerCmd the create command to customize
     */
    void customize(CreateContainerCmd createContainerCmd);
}

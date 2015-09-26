package org.testcontainers.containers;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.traits.LinkableContainer;

/**
 * Created by rnorth on 26/09/2015.
 */
public class FutureContainer implements LinkableContainer {
    private final String containerName;

    public FutureContainer(@NotNull String containerName) {
        this.containerName = containerName;
    }

    @Override
    public String getContainerName() {
        return containerName;
    }
}

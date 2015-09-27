package org.testcontainers.containers;

import lombok.Data;
import org.testcontainers.containers.traits.LinkableContainer;

/**
 * A container that may not have been launched yet.
 */
@Data
public class FutureContainer implements LinkableContainer {
    private final String containerName;
}

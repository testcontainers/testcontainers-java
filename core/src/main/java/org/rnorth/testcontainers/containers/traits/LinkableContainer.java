package org.rnorth.testcontainers.containers.traits;

/**
 * A container which can be linked to by other containers.
 */
public interface LinkableContainer {

    String getContainerId();
}

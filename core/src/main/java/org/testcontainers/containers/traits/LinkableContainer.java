package org.testcontainers.containers.traits;

/**
 * A container which can be linked to by other containers.
 *
 * @deprecated Links are deprecated (see <a href="https://github.com/testcontainers/testcontainers-java/issues/465">#465</a>). Please use {@link org.testcontainers.containers.Network} features instead.
 */
@Deprecated
public interface LinkableContainer {

    String getContainerName();
}

package org.rnorth.testcontainers.containers.traits;

/**
 * A Container Rule for a container which may be linked to.
 *
 * For example, if FooContainer exposes a port that other containers can link to,
 * and FooContainerRule launches that type of container, then FooContainerRule
 * should implement LinkableContainerRule.
 */
public interface LinkableContainerRule {

    LinkableContainer getContainer();
}

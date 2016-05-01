package org.testcontainers.utility;

public interface SelfReference<SELF extends SelfReference<SELF>> {

    /**
     * @return a reference to this container instance, cast to the expected generic type.
     */
    @SuppressWarnings("unchecked")
    default SELF self() {
        return (SELF) this;
    }
}

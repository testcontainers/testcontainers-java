package org.testcontainers.utility;

public interface SelfReference<SELF extends SelfReference<SELF>> {

    default SELF self() {
        return (SELF) this;
    }
}

package org.testcontainers.containers;

import com.github.dockerjava.api.model.AccessMode;

/**
 * Possible modes for binding storage volumes.
 */
public enum BindMode {
    READ_ONLY(AccessMode.ro), READ_WRITE(AccessMode.rw);

    public final AccessMode accessMode;

    BindMode(AccessMode accessMode) {
        this.accessMode = accessMode;
    }
}

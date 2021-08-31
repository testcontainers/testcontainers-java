package org.testcontainers.controller.model;

import com.github.dockerjava.api.model.AccessMode;

/**
 * Possible modes for binding storage volumes.
 */
public enum BindMode {
    READ_ONLY(AccessMode.ro), READ_WRITE(AccessMode.rw); // TODO: Remove AccessMode

    public final AccessMode accessMode;

    BindMode(AccessMode accessMode) {
        this.accessMode = accessMode;
    }
}

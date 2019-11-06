package org.testcontainers.utility;

import static java.util.Objects.requireNonNull;

interface Versioning {
    boolean isValid();
    String getSeparator();

    static Versioning from(String tag) {
        return requireNonNull(tag).startsWith("sha256:") ? new Sha256Versioning(tag.replace("sha256:", "")) : new TagVersioning(tag);
    }
}

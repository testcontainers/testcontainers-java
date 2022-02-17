package org.testcontainers.hivemq;

import org.jetbrains.annotations.NotNull;

class PathUtil {

    static @NotNull String prepareInnerPath(@NotNull String innerPath) {
        if ("/".equals(innerPath) || innerPath.isEmpty()) {
            return "/";
        }
        if (!innerPath.startsWith("/")) {
            innerPath = "/" + innerPath;
        }
        if (!innerPath.endsWith("/")) {
            innerPath += "/";
        }
        return innerPath;
    }

    static @NotNull String prepareAppendPath(@NotNull String appendPath) {
        if (!appendPath.startsWith("/")) {
            appendPath = "/" + appendPath;
        }
        return appendPath;
    }
}

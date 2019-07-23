package org.testcontainers.lifecycle;

import java.util.Collections;
import java.util.Set;

public interface Startable extends AutoCloseable {

    default Set<Startable> getDependencies() {
        return Collections.emptySet();
    }

    void start();

    void stop();

    @Override
    default void close() {
        stop();
    }
}

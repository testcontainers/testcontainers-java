package org.testcontainers.lifecycle;

public interface Startable extends AutoCloseable {

    void start();

    void stop();

    @Override
    default void close() {
        stop();
    }
}

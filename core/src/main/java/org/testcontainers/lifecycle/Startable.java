package org.testcontainers.lifecycle;

public interface Startable extends AutoCloseable {

    void start();

    void stop();

    void restart();

    @Override
    default void close() {
        stop();
    }
}

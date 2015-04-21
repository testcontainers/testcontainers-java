package org.rnorth.testcontainers.containers;

/**
 * @author richardnorth
 */
public interface DatabaseContainer {
    String getJdbcUrl();

    default String getUsername() {
        return "test";
    }

    default String getPassword() {
        return "test";
    }

    void start();
}

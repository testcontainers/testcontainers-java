package org.rnorth.testcontainers.containers;

/**
 * @author richardnorth
 */
public interface DatabaseContainer {

    String getName();

    String getDriverClassName();

    String getJdbcUrl();

    default String getUsername() {
        return "test";
    }

    default String getPassword() {
        return "test";
    }

    void start();

    void stop();

    void setTag(String tag);
}

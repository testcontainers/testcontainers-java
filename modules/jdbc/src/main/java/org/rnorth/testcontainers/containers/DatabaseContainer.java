package org.rnorth.testcontainers.containers;

import org.rnorth.testcontainers.containers.traits.LinkableContainer;

/**
 * @author richardnorth
 */
public interface DatabaseContainer extends LinkableContainer {

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

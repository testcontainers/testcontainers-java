package org.rnorth.testcontainers.containers;

/**
 * @author richardnorth
 */
public interface DatabaseContainer extends ManagedContainer {

    String getName();

    String getDriverClassName();

    String getJdbcUrl();

    default String getUsername() {
        return "test";
    }

    default String getPassword() {
        return "test";
    }

    void setTag(String tag);

}

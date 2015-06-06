package org.rnorth.testcontainers.containers;

/**
 * @author richardnorth
 */
public interface ManagedContainer {
    void start();

    void stop();

    void commitAndTag(String tagName);

    boolean hasExistingTag(String tagName);
}

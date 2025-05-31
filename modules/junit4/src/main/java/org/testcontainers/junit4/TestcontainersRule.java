package org.testcontainers.junit4;

import org.junit.rules.ExternalResource;
import org.testcontainers.lifecycle.Startable;

/**
 * Integrates Testcontainers with JUnit4 lifecycle.
 */
public class TestcontainersRule<T extends AutoCloseable> extends ExternalResource {

    private final T resource;

    public TestcontainersRule(T resource) {
        this.resource = resource;
    }

    /**
     * Returns the managed resource.
     * @return the managed resource
     */
    public T get() {
        return resource;
    }

    @Override
    protected void before() throws Throwable {
        super.before();
        T resource = this.resource;
        if (resource instanceof Startable) {
            ((Startable) resource).start();
        }
    }

    @Override
    protected void after() {
        try {
            resource.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

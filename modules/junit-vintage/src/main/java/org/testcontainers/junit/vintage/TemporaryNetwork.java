package org.testcontainers.junit.vintage;

import org.junit.rules.ExternalResource;
import org.testcontainers.containers.Network;

/**
 * Integrates {@link Network} with the JUnit4 lifecycle.
 */
public final class TemporaryNetwork extends ExternalResource implements Network {

    private final Network network;

    private volatile State state = State.BEFORE_RULE;

    /**
     * Creates an instance.
     *
     * <p>The passed-in network will be closed when the current test completes.
     *
     * @param network Network that the rule will delegate to.
     */
    public TemporaryNetwork(Network network) {
        this.network = network;
    }

    @Override
    public String getId() {
        if (state == State.AFTER_RULE) {
            throw new IllegalStateException("Cannot reference the network after the test completes");
        }
        return network.getId();
    }

    @Override
    public void close() {
        switch (state) {
            case BEFORE_RULE:
                throw new IllegalStateException("Cannot close the network before the test starts");
            case INSIDE_RULE:
                break;
            case AFTER_RULE:
                throw new IllegalStateException("Cannot reference the network after the test completes");
        }
        network.close();
    }

    @Override
    protected void before() throws Throwable {
        state = State.AFTER_RULE; // Just in case an exception is thrown below.
        network.getId(); // This has the side-effect of creating the network.

        state = State.INSIDE_RULE;
    }

    @Override
    protected void after() {
        state = State.AFTER_RULE;
        network.close();
    }

    private enum State {
        BEFORE_RULE,
        INSIDE_RULE,
        AFTER_RULE,
    }
}

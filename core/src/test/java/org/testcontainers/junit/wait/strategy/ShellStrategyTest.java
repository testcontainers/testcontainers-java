package org.testcontainers.junit.wait.strategy;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.testcontainers.containers.wait.strategy.ShellStrategy;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for {@link ShellStrategy}.
 */
public class ShellStrategyTest extends AbstractWaitStrategyTest<ShellStrategy> {

    private final String LOCK_FILE = "/tmp/ready.lock";

    @Test
    public void testWaitUntilReady_Success() {
        waitUntilReadyAndSucceed(String.format("touch %s; sleep 300", LOCK_FILE));
    }

    @Test
    public void testWaitUntilReady_Timeout() {
        waitUntilReadyAndTimeout("sleep 300");
    }

    @NotNull
    @Override
    protected ShellStrategy buildWaitStrategy(AtomicBoolean ready) {
        return new ShellStrategy(String.format("stat %s", LOCK_FILE)) {
            @Override
            protected void waitUntilReady() {
                super.waitUntilReady();
                ready.set(true);
            }
        };
    }
}

package org.testcontainers.containers.wait.strategy;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rnorth.ducttape.TimeoutException;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.rnorth.visibleassertions.VisibleAssertions.*;

public class WaitAllStrategyTest {

    @Mock
    private GenericContainer container;
    @Mock
    private WaitStrategy strategy1;
    @Mock
    private WaitStrategy strategy2;
    @Mock
    private WaitStrategy strategy3;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    /*
     * Dummy-based tests, to check that timeout values are propagated correctly, without involving actual timing-sensitive code
     */
    @Test
    public void parentTimeoutApplies() {

        DummyStrategy child1 = new DummyStrategy(Duration.ofMillis(10));
        child1.withStartupTimeout(Duration.ofMillis(20));

        assertEquals("withStartupTimeout directly sets the timeout", 20L, child1.startupTimeout.toMillis());

        new WaitAllStrategy()
            .withStrategy(child1)
            .withStartupTimeout(Duration.ofMillis(30));

        assertEquals("WaitAllStrategy overrides a child's timeout", 30L, child1.startupTimeout.toMillis());
    }

    @Test
    public void parentTimeoutAppliesToMultipleChildren() {

        Duration defaultInnerWait = Duration.ofMillis(2);
        Duration outerWait = Duration.ofMillis(6);

        DummyStrategy child1 = new DummyStrategy(defaultInnerWait);
        DummyStrategy child2 = new DummyStrategy(defaultInnerWait);

        new WaitAllStrategy()
            .withStrategy(child1)
            .withStrategy(child2)
            .withStartupTimeout(outerWait);

        assertEquals("WaitAllStrategy overrides a child's timeout (1st)", 6L, child1.startupTimeout.toMillis());
        assertEquals("WaitAllStrategy overrides a child's timeout (2nd)", 6L, child2.startupTimeout.toMillis());
    }

    @Test
    public void parentTimeoutAppliesToAdditionalChildren() {

        Duration defaultInnerWait = Duration.ofMillis(2);
        Duration outerWait = Duration.ofMillis(20);

        DummyStrategy child1 = new DummyStrategy(defaultInnerWait);
        DummyStrategy child2 = new DummyStrategy(defaultInnerWait);

        new WaitAllStrategy()
            .withStrategy(child1)
            .withStartupTimeout(outerWait)
            .withStrategy(child2);

        assertEquals("WaitAllStrategy overrides a child's timeout (1st)", 20L, child1.startupTimeout.toMillis());
        assertEquals("WaitAllStrategy overrides a child's timeout (2nd, additional)", 20L, child2.startupTimeout.toMillis());
    }

    /*
     * Mock-based tests to check overall behaviour, without involving timing-sensitive code
     */
    @Test
    public void childExecutionTest() {

        final WaitStrategy underTest = new WaitAllStrategy()
            .withStrategy(strategy1)
            .withStrategy(strategy2);

        doNothing().when(strategy1).waitUntilReady(eq(container));
        doNothing().when(strategy2).waitUntilReady(eq(container));

        underTest.waitUntilReady(container);

        InOrder inOrder = inOrder(strategy1, strategy2);
        inOrder.verify(strategy1).waitUntilReady(any());
        inOrder.verify(strategy2).waitUntilReady(any());
    }

    @Test
    public void withoutOuterTimeoutShouldRelyOnInnerStrategies() {

        final WaitStrategy underTest = new WaitAllStrategy(WaitAllStrategy.Mode.WITH_INDIVIDUAL_TIMEOUTS_ONLY)
            .withStrategy(strategy1)
            .withStrategy(strategy2)
            .withStrategy(strategy3);

        doNothing().when(strategy1).waitUntilReady(eq(container));
        doThrow(TimeoutException.class).when(strategy2).waitUntilReady(eq(container));

        assertThrows("The outer strategy timeout applies", TimeoutException.class, () -> {
            underTest.waitUntilReady(container);
        });

        InOrder inOrder = inOrder(strategy1, strategy2, strategy3);
        inOrder.verify(strategy1).waitUntilReady(any());
        inOrder.verify(strategy2).waitUntilReady(any());
        inOrder.verify(strategy3, never()).waitUntilReady(any());
    }

    @Test
    public void timeoutChangeShouldNotBePossibleWithIndividualTimeoutMode() {

        final WaitStrategy underTest = new WaitAllStrategy(WaitAllStrategy.Mode.WITH_INDIVIDUAL_TIMEOUTS_ONLY);

        assertThrows("Cannot change timeout for individual timeouts", IllegalStateException.class, () -> {
            underTest.withStartupTimeout(Duration.ofSeconds(42));
        });
    }

    @Test
    public void shouldNotMessWithIndividualTimeouts() {

        new WaitAllStrategy(WaitAllStrategy.Mode.WITH_INDIVIDUAL_TIMEOUTS_ONLY)
            .withStrategy(strategy1)
            .withStrategy(strategy2);

        verify(strategy1, never()).withStartupTimeout(any());
        verify(strategy1, never()).withStartupTimeout(any());
    }

    @Test
    public void shouldOverwriteIndividualTimeouts() {

        Duration someSeconds = Duration.ofSeconds(23);
        new WaitAllStrategy()
            .withStartupTimeout(someSeconds)
            .withStrategy(strategy1)
            .withStrategy(strategy2);

        verify(strategy1).withStartupTimeout(someSeconds);
        verify(strategy1).withStartupTimeout(someSeconds);
    }

    static class DummyStrategy extends AbstractWaitStrategy {
        DummyStrategy(Duration defaultInnerWait) {
            super.startupTimeout = defaultInnerWait;
        }

        @Override
        protected void waitUntilReady() {
            // no-op
        }
    }
}

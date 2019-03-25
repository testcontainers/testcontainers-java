package org.testcontainers.containers.wait.strategy;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.rnorth.visibleassertions.VisibleAssertions.*;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rnorth.ducttape.TimeoutException;
import org.testcontainers.containers.GenericContainer;

import com.google.common.util.concurrent.Uninterruptibles;

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

    @Test
    public void simpleTest() {

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
    public void maxTimeOutApplies() {

        WaitStrategy child1 = new SleepingStrategy(Duration.ofMinutes(30))
            .withStartupTimeout(Duration.ofHours(1));

        final WaitStrategy underTest = new WaitAllStrategy()
            .withStrategy(child1)
            .withStartupTimeout(Duration.ofMillis(10));

        assertThrows("The outer strategy timeout applies", TimeoutException.class, () -> {
            underTest.waitUntilReady(container);
        });
    }

    @Test
    public void appliesOuterTimeoutTooLittleTime() {

        Duration defaultInnerWait = Duration.ofMillis(2);
        Duration outerWait = Duration.ofMillis(6);

        WaitStrategy child1 = new SleepingStrategy(defaultInnerWait);
        WaitStrategy child2 = new SleepingStrategy(defaultInnerWait);

        final WaitStrategy underTest = new WaitAllStrategy()
            .withStrategy(child1)
            .withStrategy(child2)
            .withStartupTimeout(outerWait);

        assertThrows("The outer strategy timeout applies", TimeoutException.class, () -> {
            underTest.waitUntilReady(container);
        });
    }

    @Test
    public void appliesOuterTimeoutEnoughTime() {

        Duration defaultInnerWait = Duration.ofMillis(2);
        Duration outerWait = Duration.ofMillis(20);

        WaitStrategy child1 = new SleepingStrategy(defaultInnerWait);
        WaitStrategy child2 = new SleepingStrategy(defaultInnerWait);

        final WaitStrategy underTest = new WaitAllStrategy()
            .withStrategy(child1)
            .withStrategy(child2)
            .withStartupTimeout(outerWait);

        try {
            underTest.waitUntilReady(container);
        } catch (Exception e) {
            if (e.getCause() instanceof CaughtTimeoutException)
                fail("The timeout wasn't applied to inner strategies.");
            else {
                fail(e.getMessage());
            }
        }
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

        final WaitStrategy underTest = new WaitAllStrategy(WaitAllStrategy.Mode.WITH_INDIVIDUAL_TIMEOUTS_ONLY)
            .withStrategy(strategy1)
            .withStrategy(strategy2);

        verify(strategy1, never()).withStartupTimeout(any());
        verify(strategy1, never()).withStartupTimeout(any());
    }

    @Test
    public void shouldOverwriteIndividualTimeouts() {

        Duration someSeconds = Duration.ofSeconds(23);
        final WaitStrategy underTest = new WaitAllStrategy()
            .withStartupTimeout(someSeconds)
            .withStrategy(strategy1)
            .withStrategy(strategy2);

        verify(strategy1).withStartupTimeout(someSeconds);
        verify(strategy1).withStartupTimeout(someSeconds);
    }

    @Test
    public void appliesOuterTimeoutOnAdditionalChildren() {

        Duration defaultInnerWait = Duration.ofMillis(2);
        Duration outerWait = Duration.ofMillis(20);

        WaitStrategy child1 = new SleepingStrategy(defaultInnerWait);
        WaitStrategy child2 = new SleepingStrategy(defaultInnerWait);

        final WaitStrategy underTest = new WaitAllStrategy()
            .withStrategy(child1)
            .withStartupTimeout(outerWait)
            .withStrategy(child2);

        try {
            underTest.waitUntilReady(container);
        } catch (Exception e) {
            if (e.getCause() instanceof CaughtTimeoutException)
                fail("The timeout wasn't applied to inner strategies.");
            else {
                fail(e.getMessage());
            }
        }
    }

    static class CaughtTimeoutException extends RuntimeException {

        CaughtTimeoutException(String message) {
            super(message);
        }
    }

    static class SleepingStrategy extends AbstractWaitStrategy {

        private final long sleepingDuration;

        SleepingStrategy(Duration defaultInnerWait) {
            super.startupTimeout = defaultInnerWait;
            // Always oversleep by default
            this.sleepingDuration = defaultInnerWait.multipliedBy(2).toMillis();
        }

        @Override
        protected void waitUntilReady() {
            try {
                // Don't use ducttape, make sure we don't catch the wrong TimeoutException later on.
                CompletableFuture.runAsync(
                    () -> Uninterruptibles.sleepUninterruptibly(this.sleepingDuration, TimeUnit.MILLISECONDS)
                ).get((int) startupTimeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException e) {
            } catch (java.util.concurrent.TimeoutException e) {
                throw new CaughtTimeoutException("Inner wait timed out, outer strategy didn't apply.");
            }
        }
    }
}

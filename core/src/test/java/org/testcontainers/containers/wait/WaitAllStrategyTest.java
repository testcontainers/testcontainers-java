package org.testcontainers.containers.wait;

import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rnorth.ducttape.TimeoutException;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;

public class WaitAllStrategyTest {

    @Mock
    private GenericContainer container;
    @Mock
    private WaitStrategy strategy1;
    @Mock
    private WaitStrategy strategy2;

    @Before
    public void setUp() throws Exception {
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
    public void appliesOuterTimeout() {

        final WaitStrategy underTest = new WaitAllStrategy()
                .withStrategy(strategy1)
                .withStartupTimeout(Duration.ofMillis(10));

        doAnswer(invocation -> {
            Uninterruptibles.sleepUninterruptibly(10, TimeUnit.SECONDS);
            return null;
        }).when(strategy1).waitUntilReady(eq(container));

        assertThrows("The outer strategy timeout applies", TimeoutException.class, () -> {
            underTest.waitUntilReady(container);
        });
    }
}

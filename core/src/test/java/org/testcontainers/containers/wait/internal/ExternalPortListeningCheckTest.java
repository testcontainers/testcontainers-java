package org.testcontainers.containers.wait.internal;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExternalPortListeningCheckTest {

    private ServerSocket listeningSocket1;

    private ServerSocket listeningSocket2;

    private ServerSocket nonListeningSocket;

    private WaitStrategyTarget mockContainer;

    @Before
    public void setUp() throws Exception {
        listeningSocket1 = new ServerSocket(0);
        listeningSocket2 = new ServerSocket(0);

        nonListeningSocket = new ServerSocket(0);
        nonListeningSocket.close();

        mockContainer = mock(WaitStrategyTarget.class);
        when(mockContainer.getHost()).thenReturn("127.0.0.1");
    }

    @Test
    public void singleListening() {
        final ExternalPortListeningCheck check = new ExternalPortListeningCheck(
            mockContainer,
            ImmutableSet.of(listeningSocket1.getLocalPort())
        );

        final Boolean result = check.call();

        assertThat(result).as("ExternalPortListeningCheck identifies a single listening port").isTrue();
    }

    @Test
    public void multipleListening() {
        final ExternalPortListeningCheck check = new ExternalPortListeningCheck(
            mockContainer,
            ImmutableSet.of(listeningSocket1.getLocalPort(), listeningSocket2.getLocalPort())
        );

        final Boolean result = check.call();

        assertThat(result).as("ExternalPortListeningCheck identifies multiple listening port").isTrue();
    }

    @Test
    public void oneNotListening() {
        final ExternalPortListeningCheck check = new ExternalPortListeningCheck(
            mockContainer,
            ImmutableSet.of(listeningSocket1.getLocalPort(), nonListeningSocket.getLocalPort())
        );

        assertThat(catchThrowable(check::call))
            .as("ExternalPortListeningCheck detects a non-listening port among many")
            .isInstanceOf(IllegalStateException.class);
    }

    @After
    public void tearDown() throws Exception {
        listeningSocket1.close();
        listeningSocket2.close();
    }
}

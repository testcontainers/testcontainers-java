package org.testcontainers.containers.wait.internal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.rnorth.visibleassertions.VisibleAssertions;

import java.net.ServerSocket;

import static com.google.common.primitives.Ints.asList;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;

public class ExternalPortListeningCheckTest {

    private ServerSocket listeningSocket1;
    private ServerSocket listeningSocket2;
    private ServerSocket nonListeningSocket;

    @Before
    public void setUp() throws Exception {
        listeningSocket1 = new ServerSocket(0);
        listeningSocket2 = new ServerSocket(0);

        nonListeningSocket = new ServerSocket(0);
        nonListeningSocket.close();
    }

    @Test
    public void singleListening() {

        final ExternalPortListeningCheck check = new ExternalPortListeningCheck("127.0.0.1", asList(listeningSocket1.getLocalPort()));

        final Boolean result = check.call();

        VisibleAssertions.assertTrue("ExternalPortListeningCheck identifies a single listening port", result);
    }

    @Test
    public void multipleListening() {

        final ExternalPortListeningCheck check = new ExternalPortListeningCheck("127.0.0.1", asList(listeningSocket1.getLocalPort(), listeningSocket2.getLocalPort()));

        final Boolean result = check.call();

        VisibleAssertions.assertTrue("ExternalPortListeningCheck identifies multiple listening port", result);
    }

    @Test
    public void oneNotListening() {

        final ExternalPortListeningCheck check = new ExternalPortListeningCheck("127.0.0.1", asList(listeningSocket1.getLocalPort(), nonListeningSocket.getLocalPort()));

        assertThrows("ExternalPortListeningCheck detects a non-listening port among many",
                IllegalStateException.class,
                (Runnable) check::call);

    }

    @After
    public void tearDown() throws Exception {
        listeningSocket1.close();
        listeningSocket2.close();
    }
}
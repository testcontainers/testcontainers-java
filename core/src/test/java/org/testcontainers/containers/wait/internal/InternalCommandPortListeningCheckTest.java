package org.testcontainers.containers.wait.internal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import static com.google.common.primitives.Ints.asList;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

public class InternalCommandPortListeningCheckTest {

    private GenericContainer nginx;

    @Before
    public void setUp() {
        nginx = new GenericContainer<>("nginx:1.9.4");
        nginx.start();
    }

    @Test
    public void singleListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(nginx, asList(80));

        final Boolean result = check.call();

        assertTrue("InternalCommandPortListeningCheck identifies a single listening port", result);
    }

    @Test
    public void nonListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(nginx, asList(80, 1234));

        assertThrows("InternalCommandPortListeningCheck detects a non-listening port among many",
                IllegalStateException.class,
                (Runnable) check::call);
    }

    @After
    public void tearDown() {
        nginx.stop();
    }
}
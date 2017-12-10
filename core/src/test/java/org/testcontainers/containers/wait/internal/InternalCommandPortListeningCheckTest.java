package org.testcontainers.containers.wait.internal;

import com.google.common.collect.ImmutableSet;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

public class InternalCommandPortListeningCheckTest {

    @Rule
    public GenericContainer nginx = new GenericContainer<>("nginx:1.9.4");

    @Test
    public void singleListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(nginx, ImmutableSet.of(80));

        final Boolean result = check.call();

        assertTrue("InternalCommandPortListeningCheck identifies a single listening port", result);
    }

    @Test
    public void nonListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(nginx, ImmutableSet.of(80, 1234));

        assertThrows("InternalCommandPortListeningCheck detects a non-listening port among many",
                IllegalStateException.class,
                (Runnable) check::call);
    }
}
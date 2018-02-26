package org.testcontainers.containers.wait.internal;

import com.google.common.collect.ImmutableSet;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

public class InternalCommandPortListeningCheckTest {

    // Linking a custom configuration into the container so that nginx is listening on port 8080. This is necessary to proof
    // that the command formatting uses the correct casing for hexadecimal numberd (i.e. 1F90 and not 1f90).
    @Rule
    public GenericContainer nginx = new GenericContainer<>("nginx:1.9.4")
            .withClasspathResourceMapping("nginx_on_8080.conf", "/etc/nginx/conf.d/default.conf", BindMode.READ_ONLY);

    @Test
    public void singleListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(nginx, ImmutableSet.of(8080));

        final Boolean result = check.call();

        assertTrue("InternalCommandPortListeningCheck identifies a single listening port", result);
    }

    @Test
    public void nonListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(nginx, ImmutableSet.of(8080, 1234));

        assertThrows("InternalCommandPortListeningCheck detects a non-listening port among many",
                IllegalStateException.class,
                (Runnable) check::call);
    }
}
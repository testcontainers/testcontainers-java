package org.testcontainers.containers.wait.internal;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import static java.util.Arrays.asList;
import static org.rnorth.visibleassertions.VisibleAssertions.assertFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

@RunWith(Parameterized.class)
public class InternalCommandPortListeningCheckTest {

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Iterable<Object[]> data() {
        return asList(
            new Object[][]{
                {"internal-port-check-dockerfile/Dockerfile-tcp"},
                {"internal-port-check-dockerfile/Dockerfile-nc"},
                {"internal-port-check-dockerfile/Dockerfile-bash"},
            });
    }

    @Rule
    public GenericContainer container;

    public InternalCommandPortListeningCheckTest(String dockerfile) {
        container = new GenericContainer(new ImageFromDockerfile()
            .withFileFromClasspath("Dockerfile", dockerfile)
            .withFileFromClasspath("nginx.conf", "internal-port-check-dockerfile/nginx.conf")
        );
    }

    @Test
    public void singleListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(container, ImmutableSet.of(8080));

        final Boolean result = check.call();

        assertTrue("InternalCommandPortListeningCheck identifies a single listening port", result);
    }

    @Test
    public void nonListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(container, ImmutableSet.of(8080, 1234));

        final Boolean result = check.call();

        assertFalse("InternalCommandPortListeningCheck detects a non-listening port among many", result);
    }

    @Test
    public void lowAndHighPortListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(container, ImmutableSet.of(100, 8080));

        final Boolean result = check.call();

        assertTrue("InternalCommandPortListeningCheck identifies a low and a high port", result);
    }
}

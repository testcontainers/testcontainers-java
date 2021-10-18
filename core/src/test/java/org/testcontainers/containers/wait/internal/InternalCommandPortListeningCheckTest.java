package org.testcontainers.containers.wait.internal;

import com.google.common.collect.ImmutableSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

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

        Unreliables.retryUntilTrue(5, TimeUnit.SECONDS, check);

        VisibleAssertions.pass("InternalCommandPortListeningCheck identifies a single listening port");
    }

    @Test
    public void nonListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(container, ImmutableSet.of(8080, 1234));

        try {
            Unreliables.retryUntilTrue(5, TimeUnit.SECONDS, check);
            VisibleAssertions.fail("expected to fail");
        } catch (TimeoutException e) {
        }
    }

    @Test
    public void lowAndHighPortListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(container, ImmutableSet.of(100, 8080));

        Unreliables.retryUntilTrue(5, TimeUnit.SECONDS, check);

        VisibleAssertions.pass("InternalCommandPortListeningCheck identifies a low and a high port");
    }
}

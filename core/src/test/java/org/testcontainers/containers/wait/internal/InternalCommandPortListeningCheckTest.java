package org.testcontainers.containers.wait.internal;

import com.google.common.collect.ImmutableSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.utility.ducttape.TimeoutException;
import org.testcontainers.utility.ducttape.Unreliables;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.fail;

@RunWith(Parameterized.class)
public class InternalCommandPortListeningCheckTest {

    @Parameterized.Parameters(name = "{index} - {0}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                { "internal-port-check-dockerfile/Dockerfile-tcp" },
                { "internal-port-check-dockerfile/Dockerfile-nc" },
                { "internal-port-check-dockerfile/Dockerfile-bash" },
            }
        );
    }

    @Rule
    public GenericContainer container;

    public InternalCommandPortListeningCheckTest(String dockerfile) {
        container =
            new GenericContainer(
                new ImageFromDockerfile()
                    .withFileFromClasspath("Dockerfile", dockerfile)
                    .withFileFromClasspath("nginx.conf", "internal-port-check-dockerfile/nginx.conf")
            );
    }

    @Test
    public void singleListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(
            container,
            ImmutableSet.of(8080)
        );

        Unreliables.retryUntilTrue(5, TimeUnit.SECONDS, check);
    }

    @Test
    public void nonListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(
            container,
            ImmutableSet.of(8080, 1234)
        );

        try {
            Unreliables.retryUntilTrue(5, TimeUnit.SECONDS, check);
            fail("expected to fail");
        } catch (TimeoutException e) {}
    }

    @Test
    public void lowAndHighPortListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(
            container,
            ImmutableSet.of(100, 8080)
        );

        Unreliables.retryUntilTrue(5, TimeUnit.SECONDS, check);
    }
}

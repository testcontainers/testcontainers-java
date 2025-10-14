package org.testcontainers.containers.wait.internal;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.fail;

@ParameterizedClass(name = "{index} - {0}")
@MethodSource("data")
class InternalCommandPortListeningCheckTest {

    public static List<String> data() {
        return Arrays.asList(
            "internal-port-check-dockerfile/Dockerfile-tcp",
            "internal-port-check-dockerfile/Dockerfile-nc",
            "internal-port-check-dockerfile/Dockerfile-bash"
        );
    }

    public GenericContainer<?> container;

    public InternalCommandPortListeningCheckTest(String dockerfile) {
        container =
            new GenericContainer<>(
                new ImageFromDockerfile()
                    .withFileFromClasspath("Dockerfile", dockerfile)
                    .withFileFromClasspath("nginx.conf", "internal-port-check-dockerfile/nginx.conf")
            );
        container.start();
    }

    @Test
    void singleListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(
            container,
            ImmutableSet.of(8080)
        );

        Unreliables.retryUntilTrue(5, TimeUnit.SECONDS, check);
    }

    @Test
    void nonListening() {
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
    void lowAndHighPortListening() {
        final InternalCommandPortListeningCheck check = new InternalCommandPortListeningCheck(
            container,
            ImmutableSet.of(100, 8080)
        );

        Unreliables.retryUntilTrue(5, TimeUnit.SECONDS, check);
    }
}

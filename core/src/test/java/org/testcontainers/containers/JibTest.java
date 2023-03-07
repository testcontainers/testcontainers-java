package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.containers.output.OutputFrame.OutputType;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class JibTest {

    @Test
    public void useJibFluentApi() {
        try (
            // jibContainerUsage {
            GenericContainer<?> busybox = new GenericContainer<>(
                new JibImage(
                    "busybox:1.35",
                    jibContainerBuilder -> jibContainerBuilder.setEntrypoint("echo", "Hello World")
                )
            )
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(3)))
            // }
        ) {
            busybox.start();
            String logs = busybox.getLogs(OutputType.STDOUT);
            assertThat(logs).contains("Hello World");
        }
    }
}

package org.testcontainers.containers;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FixedHostPortGenericContainerTest {
    @Test
    public void shouldExposePort() throws Exception {
        try (FixedHostPortGenericContainer<?> container = new FixedHostPortGenericContainer<>("alpine:3.7")) {
            container
                .withCommand("sh", "-c", "while true; do nc -lp 8734; done")
                .withFixedExposedPort(53834, 8734);

            assertThat(container.getExposedPorts())
                .contains(8734);
        }
    }
}

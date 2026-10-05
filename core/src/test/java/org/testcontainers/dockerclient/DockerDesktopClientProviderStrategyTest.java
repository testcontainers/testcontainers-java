package org.testcontainers.dockerclient;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

class DockerDesktopClientProviderStrategyTest {

    /**
     * The lazily resolved socket path should be {@code null} when no Docker Desktop
     * socket is present under {@code user.home}.
     */
    @Test
    void socketPathIsNullWhenNoDockerDesktopSocketExists() throws Exception {
        String originalHome = System.getProperty("user.home");
        Path emptyHome = Files.createTempDirectory("tc-no-docker-desktop");
        System.setProperty("user.home", emptyHome.toString());
        try {
            assertThat(new DockerDesktopClientProviderStrategy().getSocketPath()).isNull();
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    /**
     * Regression test for #11829: previously {@code isApplicable()} always returned
     * {@code true} on Linux/macOS because {@code @Getter(lazy = true)} turns the field
     * into an {@code AtomicReference}, whose instance is never {@code null}.
     *
     * <p>Only meaningful on Linux/macOS, where the strategy is eligible in the first place.
     */
    @Test
    void notApplicableWhenNoDockerDesktopSocketExists() throws Exception {
        assumeThat(SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC).isTrue();

        String originalHome = System.getProperty("user.home");
        Path emptyHome = Files.createTempDirectory("tc-no-docker-desktop");
        System.setProperty("user.home", emptyHome.toString());
        try {
            DockerDesktopClientProviderStrategy strategy = new DockerDesktopClientProviderStrategy();

            assertThat(strategy.getSocketPath()).isNull();
            assertThat(strategy.isApplicable()).isFalse();
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }
}

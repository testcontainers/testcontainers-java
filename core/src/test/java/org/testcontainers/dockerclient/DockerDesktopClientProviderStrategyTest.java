package org.testcontainers.dockerclient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("deprecation")
class DockerDesktopClientProviderStrategyTest {

    private final String originalUserHome = System.getProperty("user.home");

    @AfterEach
    void restoreUserHome() {
        System.setProperty("user.home", originalUserHome);
    }

    @Test
    @EnabledOnOs({ OS.LINUX, OS.MAC })
    void notApplicableWhenDockerDesktopSocketIsMissing(@TempDir Path userHome) {
        // user.home without a Docker Desktop socket under .docker/desktop or .docker/run
        System.setProperty("user.home", userHome.toString());

        DockerDesktopClientProviderStrategy strategy = new DockerDesktopClientProviderStrategy();

        assertThat(strategy.isApplicable()).isFalse();
    }

    @Test
    @EnabledOnOs({ OS.LINUX, OS.MAC })
    void applicableWhenDockerDesktopSocketExists(@TempDir Path userHome) throws IOException {
        Path socketPath = userHome.resolve(".docker").resolve("desktop").resolve("docker.sock");
        Files.createDirectories(socketPath.getParent());
        Files.createFile(socketPath);
        System.setProperty("user.home", userHome.toString());

        DockerDesktopClientProviderStrategy strategy = new DockerDesktopClientProviderStrategy();

        assertThat(strategy.isApplicable()).isTrue();
    }
}

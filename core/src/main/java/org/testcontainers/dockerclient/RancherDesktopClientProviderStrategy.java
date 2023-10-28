package org.testcontainers.dockerclient;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Look at the following paths:
 * <ul>
 *     <li>Linux: ~/.rd/docker.sock</li>
 *     <li>MacOS: ~/.rd/docker.sock</li>
 * </ul>
 *
 * @deprecated this class is used by the SPI and should not be used directly
 */
@Slf4j
@Deprecated
public class RancherDesktopClientProviderStrategy extends DockerClientProviderStrategy {

    public static final int PRIORITY = UnixSocketClientProviderStrategy.PRIORITY - 1;

    @Getter(lazy = true)
    @Nullable
    private final Path socketPath = resolveSocketPath();

    private Path resolveSocketPath() {
        Path linuxPath = Paths.get(System.getProperty("user.home")).resolve(".rd");
        return tryFolder(linuxPath)
            .orElseGet(() -> {
                Path macosPath = Paths.get(System.getProperty("user.home")).resolve(".rd");
                return tryFolder(macosPath).orElse(null);
            });
    }

    @Override
    public String getDescription() {
        return "Docker accessed via Unix socket (" + getSocketPath() + ")";
    }

    @Override
    public TransportConfig getTransportConfig() throws InvalidConfigurationException {
        return TransportConfig.builder().dockerHost(URI.create("unix://" + getSocketPath().toString())).build();
    }

    @Override
    protected int getPriority() {
        return PRIORITY;
    }

    @Override
    protected boolean isPersistable() {
        return false;
    }

    @Override
    public String getRemoteDockerUnixSocketPath() {
        return "/var/run/docker.sock";
    }

    @Override
    protected boolean isApplicable() {
        return ((SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC) && this.socketPath != null);
    }

    private Optional<Path> tryFolder(Path path) {
        if (!Files.exists(path)) {
            log.debug("'{}' does not exist.", path);
            return Optional.empty();
        }
        Path socketPath = path.resolve("docker.sock");
        if (!Files.exists(socketPath)) {
            log.debug("'{}' does not exist.", socketPath);
            return Optional.empty();
        }
        return Optional.of(socketPath);
    }
}

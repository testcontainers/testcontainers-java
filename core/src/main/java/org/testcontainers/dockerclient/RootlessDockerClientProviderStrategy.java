package org.testcontainers.dockerclient;

import com.sun.jna.Library;
import com.sun.jna.Native;
import org.apache.commons.lang.SystemUtils;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @deprecated this class is used by the SPI and should not be used directly
 */
@Deprecated
public final class RootlessDockerClientProviderStrategy extends DockerClientProviderStrategy {

    public static final int PRIORITY = UnixSocketClientProviderStrategy.PRIORITY + 1;

    private Path getSocketPath() {
        String xdgRuntimeDir = System.getenv("XDG_RUNTIME_DIR");
        if (xdgRuntimeDir == null) {
            xdgRuntimeDir = "/run/user/" + LibC.INSTANCE.getuid();
        }
        return Paths.get(xdgRuntimeDir).resolve("docker.sock");
    }

    @Override
    public TransportConfig getTransportConfig() throws InvalidConfigurationException {
        return TransportConfig.builder()
            .dockerHost(URI.create("unix://" + getSocketPath().toString()))
            .build();
    }

    @Override
    protected boolean isApplicable() {
        return SystemUtils.IS_OS_LINUX && Files.exists(getSocketPath());
    }

    @Override
    public String getDescription() {
        return "Rootless Docker accessed via Unix socket (" + getSocketPath() + ")";
    }

    @Override
    protected int getPriority() {
        return PRIORITY;
    }

    private interface LibC extends Library {

        LibC INSTANCE = Native.loadLibrary("c", LibC.class);

        int getuid();
    }

}

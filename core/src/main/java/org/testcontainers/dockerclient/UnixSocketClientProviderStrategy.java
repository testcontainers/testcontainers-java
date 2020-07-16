package org.testcontainers.dockerclient;

import org.apache.commons.lang.SystemUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @deprecated this class is used by the SPI and should not be used directly
 */
@Deprecated
public final class UnixSocketClientProviderStrategy extends DockerClientProviderStrategy {
    protected static final String DOCKER_SOCK_PATH = "/var/run/docker.sock";
    private static final String SOCKET_LOCATION = "unix://" + DOCKER_SOCK_PATH;
    private static final int SOCKET_FILE_MODE_MASK = 0xc000;

    public static final int PRIORITY = EnvironmentAndSystemPropertyClientProviderStrategy.PRIORITY - 20;

    @Override
    public TransportConfig getTransportConfig() throws InvalidConfigurationException {
        Path dockerSocketFile = Paths.get(DOCKER_SOCK_PATH);
        Integer mode;
        try {
            mode = (Integer) Files.getAttribute(dockerSocketFile, "unix:mode");
        } catch (IOException e) {
            throw new InvalidConfigurationException("Could not find unix domain socket", e);
        }

        if ((mode & 0xc000) != SOCKET_FILE_MODE_MASK) {
            throw new InvalidConfigurationException("Found docker unix domain socket but file mode was not as expected (expected: srwxr-xr-x). This problem is possibly due to occurrence of this issue in the past: https://github.com/docker/docker/issues/13121");
        }

        return TransportConfig.builder()
            .dockerHost(URI.create(SOCKET_LOCATION))
            .build();
    }

    @Override
    protected boolean isApplicable() {
        return SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC;
    }

    @Override
    public String getDescription() {
        return "local Unix socket (" + SOCKET_LOCATION + ")";
    }

    @Override
    protected int getPriority() {
        return PRIORITY;
    }
}

package org.testcontainers.dockerclient;

import org.apache.commons.lang.SystemUtils;

import java.net.URI;

/**
 *
 * @deprecated this class is used by the SPI and should not be used directly
 */
@Deprecated
public final class NpipeSocketClientProviderStrategy extends DockerClientProviderStrategy {

    protected static final String DOCKER_SOCK_PATH = "//./pipe/docker_engine";
    private static final String SOCKET_LOCATION = "npipe://" + DOCKER_SOCK_PATH;

    public static final int PRIORITY = EnvironmentAndSystemPropertyClientProviderStrategy.PRIORITY - 20;

    @Override
    public TransportConfig getTransportConfig() {
        return TransportConfig.builder()
            .dockerHost(URI.create(SOCKET_LOCATION))
            .build();
    }

    @Override
    protected boolean isApplicable() {
        return SystemUtils.IS_OS_WINDOWS;
    }

    @Override
    public String getDescription() {
        return "local Npipe socket (" + SOCKET_LOCATION + ")";
    }

    @Override
    protected int getPriority() {
        return PRIORITY;
    }
}

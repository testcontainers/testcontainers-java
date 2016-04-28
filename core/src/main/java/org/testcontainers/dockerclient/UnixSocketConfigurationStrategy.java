package org.testcontainers.dockerclient;

/**
 * Attempt to configure docker using a local Unix socket.
 *
 * @author richardnorth
 */
public class UnixSocketConfigurationStrategy extends AbstractSocketConfigurationStrategy {
    public static final String SOCKET_LOCATION = "unix:///var/run/docker.sock";
    private static final String SOCKET_DESCRIPTION = "local Unix";

    @Override
    protected String getSocketLocation() {
        return SOCKET_LOCATION;
    }

    @Override
    protected String getSocketDescription() {
        return SOCKET_DESCRIPTION;
    }
}

package org.testcontainers.dockerclient;

/**
 * Attempt to configure docker using {@literal docker.local}.
 *
 * @author pcornish
 */
public class DockerLocalConfigurationStrategy extends AbstractSocketConfigurationStrategy {
    public static final String SOCKET_LOCATION = "tcp://docker.local:2375";
    private static final String SOCKET_DESCRIPTION = "docker.local";

    @Override
    protected String getSocketLocation() {
        return SOCKET_LOCATION;
    }

    @Override
    protected String getSocketDescription() {
        return SOCKET_DESCRIPTION;
    }
}

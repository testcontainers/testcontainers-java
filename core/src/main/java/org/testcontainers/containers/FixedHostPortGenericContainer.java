package org.testcontainers.containers;

import org.jetbrains.annotations.NotNull;

/**
 * Variant of {@link GenericContainer} that allows a fixed port on the docker host to be mapped to a container port.
 *
 * <p><strong>Normally this should not be required, and Docker should be allowed to choose a free host port instead</strong>.
 * However, when a fixed host port is absolutely required for some reason, this class can be used to set it.</p>
 *
 * <p>Callers are responsible for ensuring that this fixed port is actually available; failure will occur if it is
 * not available - which could manifest as flaky or unstable tests.</p>
 */
public class FixedHostPortGenericContainer<SELF extends FixedHostPortGenericContainer<SELF>> extends GenericContainer<SELF> {

    /**
     * @deprecated it is highly recommended that {@link FixedHostPortGenericContainer} not be used, as it risks port conflicts.
     */
    @Deprecated
    public FixedHostPortGenericContainer(@NotNull String dockerImageName) {
        super(dockerImageName);
    }

    /**
     * Bind a fixed TCP port on the docker host to a container port
     * @param hostPort          a port on the docker host, which must be available
     * @param containerPort     a port in the container
     * @return                  this container
     */
    public SELF withFixedExposedPort(int hostPort, int containerPort) {

        return withFixedExposedPort(hostPort, containerPort, InternetProtocol.TCP);
    }

    /**
     * Bind a fixed port on the docker host to a container port
     * @param hostPort          a port on the docker host, which must be available
     * @param containerPort     a port in the container
     * @param protocol          an internet protocol (tcp or udp)
     * @return                  this container
     */
    public SELF withFixedExposedPort(int hostPort, int containerPort, InternetProtocol protocol) {

        super.addFixedExposedPort(hostPort, containerPort, protocol);

        return self();
    }
}

package org.testcontainers.utility;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

class RyukContainer extends GenericContainer<RyukContainer> {

    private static final int DEFAULT_PORT = 8080;

    private final Integer listeningPort;

    RyukContainer() {
        super("testcontainers/ryuk:0.14.0");
        String network = TestcontainersConfiguration.getInstance().getRyukNetwork();
        Integer configuredPort = configuredPort();
        if ("host".equals(network)) {
            // In host network mode Docker does not publish ports, so there is no mapped port to
            // read back and the daemon offers no way to allocate a port in the Docker host's
            // network namespace. Ryuk therefore listens on its default port, or the one
            // configured via ryuk.container.port, directly in the Docker host's namespace. No
            // port is probed in the JVM, whose network namespace can differ from the Docker
            // host's (e.g. a bridge-networked JVM or a remote daemon), so a locally free port
            // could already be occupied on the host. If the port is occupied, Ryuk fails to bind
            // and exits before logging "Started", failing the startup loudly instead of silently
            // taking the port of another process. No ports are exposed, as they would never
            // become mapped in this mode and the mapped-port wait in GenericContainer#tryStart
            // would time out.
            withNetworkMode("host");
            this.listeningPort = configuredPort != null ? configuredPort : DEFAULT_PORT;
        } else {
            // On networks where Docker publishes ports, the daemon picks a free host-side port
            // in its own namespace; the JVM only reads it back after startup.
            this.listeningPort = null;
            withExposedPorts(configuredPort != null ? configuredPort : DEFAULT_PORT);
            if (network != null) {
                withNetworkMode(network);
            }
        }
        if (configuredPort != null) {
            withEnv("RYUK_PORT", String.valueOf(configuredPort));
        }
        withCreateContainerCmdModifier(cmd -> {
            cmd.withName("testcontainers-ryuk-" + DockerClientFactory.SESSION_ID);
            cmd.withHostConfig(
                cmd
                    .getHostConfig()
                    .withAutoRemove(true)
                    .withPrivileged(TestcontainersConfiguration.getInstance().isRyukPrivileged())
                    .withBinds(
                        new Bind(
                            DockerClientFactory.instance().getRemoteDockerUnixSocketPath(),
                            new Volume("/var/run/docker.sock")
                        )
                    )
            );
        });

        waitingFor(Wait.forLogMessage(".*Started.*", 1));
    }

    /**
     * Returns the port that the Ryuk process listens on, reachable at {@link #getHost()}.
     * <p>
     * When Ryuk is attached to the <code>host</code> network, it listens directly on the Docker
     * host, using the port configured via <code>ryuk.container.port</code> or Ryuk's default
     * port. For any other network, the mapped (published) port chosen by the Docker daemon is
     * used.
     *
     * @return the port to connect to Ryuk at {@link #getHost()}
     */
    Integer getPort() {
        if (listeningPort != null) {
            return listeningPort;
        }
        return getFirstMappedPort();
    }

    @Nullable
    private static Integer configuredPort() {
        Integer port = TestcontainersConfiguration.getInstance().getRyukPort();
        if (port != null && (port < 1 || port > 65535)) {
            throw new IllegalArgumentException("ryuk.container.port must be between 1 and 65535, but is " + port);
        }
        return port;
    }
}

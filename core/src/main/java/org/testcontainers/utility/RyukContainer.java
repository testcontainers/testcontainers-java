package org.testcontainers.utility;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import java.io.IOException;
import java.net.ServerSocket;
import javax.net.ServerSocketFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

class RyukContainer extends GenericContainer<RyukContainer> {

    private static final int DEFAULT_PORT = 8080;

    private final Integer listeningPort;

    RyukContainer() {
        super("testcontainers/ryuk:0.14.0");
        String network = TestcontainersConfiguration.getInstance().getRyukNetwork();
        if ("host".equals(network)) {
            // In host network mode Docker does not publish ports, so Ryuk listens on a host port
            // directly. Pick a free port to avoid colliding with anything already bound to the
            // default port, and pass it to Ryuk. Do not declare exposed ports, as they would never
            // become mapped in this mode, and the mapped-port wait in GenericContainer#tryStart
            // would time out.
            this.listeningPort = findFreePort();
            withEnv("RYUK_PORT", String.valueOf(listeningPort));
            withNetworkMode("host");
        } else {
            this.listeningPort = null;
            withExposedPorts(DEFAULT_PORT);
            if (network != null) {
                withNetworkMode(network);
            }
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
     * When Ryuk is attached to the <code>host</code> network, it listens on a host port chosen
     * before startup (passed to Ryuk via the <code>RYUK_PORT</code> environment variable). For any
     * other network, the mapped (published) port is used.
     *
     * @return the port to connect to Ryuk at {@link #getHost()}
     */
    Integer getPort() {
        if (listeningPort != null) {
            return listeningPort;
        }
        return getFirstMappedPort();
    }

    private static Integer findFreePort() {
        try (ServerSocket socket = ServerSocketFactory.getDefault().createServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            // Failing fast is safer than silently falling back to Ryuk's default port,
            // which may already be occupied: Ryuk would fail to bind with a confusing
            // error, or steal the port from its owner.
            throw new IllegalStateException("Failed to find a free host port for Ryuk", e);
        }
    }
}

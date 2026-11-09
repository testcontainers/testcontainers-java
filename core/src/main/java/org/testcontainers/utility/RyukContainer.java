package org.testcontainers.utility;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

class RyukContainer extends GenericContainer<RyukContainer> {

    RyukContainer() {
        super("testcontainers/ryuk:0.14.0");
        withExposedPorts(8080);
        String network = TestcontainersConfiguration.getInstance().getRyukNetwork();
        if (network != null) {
            withNetworkMode(network);
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
     * When Ryuk is attached to the <code>host</code> network, Docker does not publish ports, so the exposed port is
     * used directly. For any other network, the mapped (published) port is used.
     *
     * @return the port to connect to Ryuk at {@link #getHost()}
     */
    Integer getPort() {
        if ("host".equals(getNetworkMode())) {
            return getExposedPorts().get(0);
        }
        return getFirstMappedPort();
    }
}

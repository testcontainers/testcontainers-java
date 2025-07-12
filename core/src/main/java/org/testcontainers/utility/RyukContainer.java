package org.testcontainers.utility;

import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

class RyukContainer extends GenericContainer<RyukContainer> {

    RyukContainer() {
        super("testcontainers/ryuk:0.12.0");
        withExposedPorts(8080);
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
}

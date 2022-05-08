package org.testcontainers.utility;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.time.Duration;

class RyukContainer extends GenericContainer<RyukContainer> {

    RyukContainer() {
        super("testcontainers/ryuk:0.3.3");

        withExposedPorts(8080);
        withCreateContainerCmdModifier(cmd -> {
            cmd.withName("testcontainers-ryuk-" + DockerClientFactory.SESSION_ID);
            cmd.withHostConfig(
                cmd.getHostConfig()
                    .withAutoRemove(true)
                    .withPrivileged(TestcontainersConfiguration.getInstance().isRyukPrivileged())
            );
        });
        getBinds().add(new Bind(DockerClientFactory.instance().getRemoteDockerUnixSocketPath(), new Volume("/var/run/docker.sock")));

        withStartupCheckStrategy(new StartupCheckStrategy() {
            @Override
            public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
                return StartupStatus.SUCCESSFUL;
            }
        });

        waitingFor(new WaitStrategy() {
            @Override
            public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {

            }

            @Override
            public WaitStrategy withStartupTimeout(Duration startupTimeout) {
                return this;
            }
        });
    }
}

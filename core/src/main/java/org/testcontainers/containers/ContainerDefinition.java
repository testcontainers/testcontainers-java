package org.testcontainers.containers;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.function.Consumer;

interface ContainerDefinition {
    StartedContainer run();

    Builder mutate();

    void apply(CreateContainerCmd createContainerCmd);

    static ContainerDefinition.Builder builder() {
        return new DefaultContainerDefinitionBuilder();
    }

    interface Builder {
        Builder image(DockerImageName image);

        Builder image(String image);

        Builder exposePort(ExposedPort exposedPort);

        Builder exposePorts(ExposedPort... ports);

        Builder exposeTcpPort(int port);

        Builder exposeTcpPorts(int... ports);

        Builder portBindings(PortBinding... portBindings);

        Builder label(String key, String value);

        Builder labels(Map<String, String> labels);

        Builder binds(Bind... binds);

        Builder environmentVariable(String key, String value);

        Builder environmentVariables(Map<String, String> environmentVariables);

        Builder command(String... commandParts);

        Builder network(Network network);

        Builder networkAliases(String... aliases);

        Builder networkMode(String networkMode);

        Builder privilegedMode(boolean privilegedMode);

        Builder dependsOn(Startable... startables);

        Builder waitStrategy(WaitStrategy waitStrategy);

        Builder createContainerCmdModifier(
            Consumer<com.github.dockerjava.api.command.CreateContainerCmd> createContainerCmdConsumer
        );

        Builder apply(Consumer<Builder> builderConsumer);

        ContainerDefinition build();
    }

    interface StartedContainer extends AutoCloseable {
        String getContainerId();

        String getHost();

        Integer getPort(int port);

        void destroy();

        @Override
        default void close() {
            destroy();
        }
    }
}

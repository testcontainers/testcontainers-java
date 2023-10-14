package org.testcontainers.containers;

import com.github.dockerjava.api.command.CreateContainerCmd;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.util.function.Consumer;

class DefaultContainerDefinition implements ContainerDefinition {

    private final DockerImageName image;

    private final Consumer<CreateContainerCmd> createContainerCmdConsumer;

    private final WaitStrategy waitStrategy;

    private final DefaultContainerDefinitionBuilder builder;

    private GenericContainer<?> container;

    DefaultContainerDefinition(
        DockerImageName image,
        Consumer<CreateContainerCmd> createContainerCmdConsumer,
        WaitStrategy waitStrategy,
        DefaultContainerDefinitionBuilder builder
    ) {
        this.image = image;
        this.createContainerCmdConsumer = createContainerCmdConsumer;
        this.waitStrategy = waitStrategy == null ? new HostPortWaitStrategy() : waitStrategy;
        this.builder = builder;
    }

    @Override
    public StartedContainer run() {
        return new DefaultRunContainerSpec();
    }

    @Override
    public Builder mutate() {
        return new DefaultContainerDefinitionBuilder(this.builder);
    }

    @Override
    public void apply(CreateContainerCmd createContainerCmd) {
        this.createContainerCmdConsumer.accept(createContainerCmd);
    }

    class DefaultRunContainerSpec implements StartedContainer {

        @Override
        public String getContainerId() {
            return container.getContainerId();
        }

        @Override
        public String getHost() {
            return container.getHost();
        }

        @Override
        public Integer getPort(int port) {
            return container.getMappedPort(port);
        }

        @Override
        public void destroy() {
            container.stop();
        }
    }
}

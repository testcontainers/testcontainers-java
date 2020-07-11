package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * Class to provide a wait strategy target for services started through docker-compose
 */
@EqualsAndHashCode
class ComposeServiceWaitStrategyTarget implements WaitStrategyTarget {

    private final Container container;
    private final GenericContainer proxyContainer;
    @NonNull
    private Map<Port, Integer> mappedPorts;
    @Getter(lazy = true)
    private final InspectContainerResponse containerInfo = DockerClientFactory.instance().client().inspectContainerCmd(getContainerId()).exec();

    ComposeServiceWaitStrategyTarget(Container container, GenericContainer proxyContainer,
                                     @NonNull Map<Port, Integer> mappedPorts) {
        this.container = container;
        this.proxyContainer = proxyContainer;
        this.mappedPorts = mappedPorts;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> getExposedPorts() {
        return this.mappedPorts.keySet()
            .stream()
            .map(Port::getValue)
            .collect(Collectors.toList());
    }

    @Override
    public Set<Port> exposedPorts() {
        return new HashSet<>(mappedPorts.keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getMappedPort(int originalPort) {
        return this.getMappedPort(originalPort, InternetProtocol.TCP);
    }

    @Override
    public Integer getMappedPort(int originalPort, InternetProtocol internetProtocol) {
        return this.proxyContainer.getMappedPort(this.mappedPorts.get(Port.of(originalPort, internetProtocol)), internetProtocol);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHost() {
        return proxyContainer.getHost();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContainerId() {
        return this.container.getId();
    }
}

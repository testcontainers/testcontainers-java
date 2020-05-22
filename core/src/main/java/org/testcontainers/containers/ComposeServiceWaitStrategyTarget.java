package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Class to provide a wait strategy target for services started through docker-compose
 */
@EqualsAndHashCode
class ComposeServiceWaitStrategyTarget implements WaitStrategyTarget {

    private final Container container;
    private final GenericContainer proxyContainer;
    @NonNull
    private Map<Integer, Integer> mappedPorts;
    @Getter(lazy=true)
    private final InspectContainerResponse containerInfo = DockerClientFactory.instance().client().inspectContainerCmd(getContainerId()).exec();

    ComposeServiceWaitStrategyTarget(Container container, GenericContainer proxyContainer,
                                     @NonNull Map<Integer, Integer> mappedPorts) {
        this.container = container;
        this.proxyContainer = proxyContainer;
        this.mappedPorts = new HashMap<>(mappedPorts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Integer> getExposedPorts() {
        return new ArrayList<>(this.mappedPorts.keySet());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getMappedPort(int originalPort) {
        return this.proxyContainer.getMappedPort(this.mappedPorts.get(originalPort));
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

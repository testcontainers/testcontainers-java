package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.slf4j.Logger;
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
    @Getter
    private final Logger logger;
    @NonNull
    private Map<Integer, Integer> mappedPorts;
    @Getter
    private List<Integer> exposedPorts = new ArrayList<>();
    @Getter(lazy=true)
    private final InspectContainerResponse containerInfo = DockerClientFactory.instance().client().inspectContainerCmd(getContainerId()).exec();

    ComposeServiceWaitStrategyTarget(Container container, GenericContainer proxyContainer,
                                     Logger logger, @NonNull Map<Integer, Integer> mappedPorts) {
        this.container = container;

        this.proxyContainer = proxyContainer;
        this.logger = logger;
        this.mappedPorts = new HashMap<>(mappedPorts);
        this.exposedPorts.addAll(this.mappedPorts.keySet());
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
    public String getContainerIpAddress() {
        return proxyContainer.getContainerIpAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getContainerId() {
        return this.container.getId();
    }
}

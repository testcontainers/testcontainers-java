package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.google.common.base.Preconditions;
import org.testcontainers.DockerClientFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface ContainerState {

    String STATE_HEALTHY = "healthy";

    /**
     * Get the IP address that this container may be reached on (may not be the local machine).
     *
     * @return an IP address
     */
    default String getContainerIpAddress() {
        return DockerClientFactory.instance().dockerHostIpAddress();
    }

    /**
     * @return is the container currently running?
     */
    default boolean isRunning() {
        if (getContainerId() == null) {
            return false;
        }

        try {
            Boolean running = getCurrentContainerInfo().getState().getRunning();
            return Boolean.TRUE.equals(running);
        } catch (DockerException e) {
            return false;
        }
    }

    /**
     * @return has the container health state 'healthy'?
     */
    default boolean isHealthy() {
        if (getContainerId() == null) {
            return false;
        }

        try {
            InspectContainerResponse inspectContainerResponse = getCurrentContainerInfo();
            String healthStatus = inspectContainerResponse.getState().getHealth().getStatus();

            return healthStatus.equals(STATE_HEALTHY);
        } catch (DockerException e) {
            return false;
        }
    }

    default InspectContainerResponse getCurrentContainerInfo() {
        return DockerClientFactory.instance().client().inspectContainerCmd(getContainerId()).exec();
    }

    /**
     * Get the actual mapped port for a first port exposed by the container.
     *
     * @return the port that the exposed port is mapped to
     * @throws IllegalStateException if there are no exposed ports
     */
    default Integer getFirstMappedPort() {
        return getExposedPorts()
            .stream()
            .findFirst()
            .map(this::getMappedPort)
            .orElseThrow(() -> new IllegalStateException("Container doesn't expose any ports"));
    }

    /**
     * Get the actual mapped port for a given port exposed by the container.
     *
     * @param originalPort the original TCP port that is exposed
     * @return the port that the exposed port is mapped to, or null if it is not exposed
     */
    default Integer getMappedPort(int originalPort) {
        Preconditions.checkState(this.getContainerId() != null, "Mapped port can only be obtained after the container is started");

        Ports.Binding[] binding = new Ports.Binding[0];
        final InspectContainerResponse containerInfo = this.getContainerInfo();
        if (containerInfo != null) {
            binding = containerInfo.getNetworkSettings().getPorts().getBindings().get(new ExposedPort(originalPort));
        }

        if (binding != null && binding.length > 0 && binding[0] != null) {
            return Integer.valueOf(binding[0].getHostPortSpec());
        } else {
            throw new IllegalArgumentException("Requested port (" + originalPort + ") is not mapped");
        }
    }

    /**
     * @return the exposed ports
     */
    List<Integer> getExposedPorts();

    /**
     * @return the port bindings
     */
    default List<String> getPortBindings() {
        List<String> portBindings = new ArrayList<>();
        final Ports hostPortBindings = this.getContainerInfo().getHostConfig().getPortBindings();
        for (Map.Entry<ExposedPort, Ports.Binding[]> binding : hostPortBindings.getBindings().entrySet()) {
            for (Ports.Binding portBinding : binding.getValue()) {
                portBindings.add(String.format("%s:%s", portBinding.toString(), binding.getKey()));
            }
        }
        return portBindings;
    }

    /**
     * @return the bound port numbers
     */
    default List<Integer> getBoundPortNumbers() {
        return getPortBindings().stream()
            .map(PortBinding::parse)
            .map(PortBinding::getBinding)
            .map(Ports.Binding::getHostPortSpec)
            .map(Integer::valueOf)
            .collect(Collectors.toList());
    }

    /**
     * @return the id of the container
     */
    String getContainerId();

    /**
     * @return the container info
     */
    InspectContainerResponse getContainerInfo();
}

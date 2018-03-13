package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.testcontainers.DockerClientFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface ContainerState {

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
    default Boolean isRunning() {
        try {
            return getContainerId() != null && DockerClientFactory.instance().client().inspectContainerCmd(getContainerId()).exec().getState().getRunning();
        } catch (DockerException e) {
            return false;
        }
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
    Integer getMappedPort(int originalPort);

    /**
     * @return the exposed ports
     */
    List<Integer> getExposedPorts();

    /**
     * @return the container exposed port numbers mapped to ports exposed on the docker host
     */
    default List<Integer> getExposedPortNumbers() {
        return getExposedPorts().stream()
            .map(this::getMappedPort)
            .collect(Collectors.toList());
    }

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
     * @return the name of the container
     */
    default String getContainerName() {
        return getContainerInfo().getName();
    }

    /**
     * @return the container info
     */
    InspectContainerResponse getContainerInfo();
}

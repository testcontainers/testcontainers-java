package org.testcontainers.docker.model;

import org.testcontainers.controller.model.Binding;
import org.testcontainers.controller.model.ExposedPort;
import org.testcontainers.controller.model.Ports;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class DockerPorts implements Ports {
    private final com.github.dockerjava.api.model.Ports ports;

    public DockerPorts(com.github.dockerjava.api.model.Ports ports) {
        this.ports = ports;
    }

    @Override
    public Map<ExposedPort, Binding[]> getBindings() {
        return ports.getBindings().entrySet().stream()
            .collect(Collectors.toMap(
                e -> new DockerExposedPort(e.getKey()),
                e -> {
                    if(e.getValue() == null) {
                        return new Binding[0];
                    }
                    return Arrays.stream(e.getValue()).map(DockerBinding::new).toArray(Binding[]::new);
                }
            ));
    }

    @Override
    public Binding[] getBindings(int port) {
        return getBindings().entrySet().stream()
            .filter(e -> e.getKey().getPort() == port)
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(new Binding[0]);
    }
}

package org.testcontainers.docker.model;

import com.github.dockerjava.api.model.Ports;
import org.testcontainers.controller.model.Binding;

public class DockerBinding implements Binding {
    private final Ports.Binding binding;

    public DockerBinding(Ports.Binding binding) {
        this.binding = binding;
    }

    @Override
    public Integer getHostPort() {
        return Integer.valueOf(binding.getHostPortSpec());
    }
}

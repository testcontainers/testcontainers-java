package org.testcontainers.docker.model;

import com.github.dockerjava.api.model.ContainerConfig;
import org.testcontainers.controller.model.ImageConfig;

import java.util.Map;

public class DockerImageConfig implements ImageConfig {
    private final ContainerConfig config;

    public DockerImageConfig(ContainerConfig config) {
        this.config = config;
    }

    @Override
    public Map<String, String> getLabels() {
        return config.getLabels();
    }
}

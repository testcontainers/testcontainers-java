package org.testcontainers.docker.model;

import org.testcontainers.controller.model.ExposedPort;

import java.util.Objects;

public class DockerExposedPort implements ExposedPort {


    private final com.github.dockerjava.api.model.ExposedPort exposedPort;

    public DockerExposedPort(com.github.dockerjava.api.model.ExposedPort exposedPort) {
        this.exposedPort = exposedPort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DockerExposedPort that = (DockerExposedPort) o;
        return Objects.equals(exposedPort, that.exposedPort);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exposedPort);
    }

    @Override
    public int getPort() {
        return exposedPort.getPort();
    }
}

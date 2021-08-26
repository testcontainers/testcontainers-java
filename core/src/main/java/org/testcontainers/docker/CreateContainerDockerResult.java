package org.testcontainers.docker;

import com.github.dockerjava.api.command.CreateContainerResponse;
import org.testcontainers.controller.CreateContainerResult;

public class CreateContainerDockerResult implements CreateContainerResult {

    private final CreateContainerResponse exec;

    public CreateContainerDockerResult(CreateContainerResponse exec) {
        this.exec = exec;
    }

    @Override
    public String getId() {
        return exec.getId();
    }
}

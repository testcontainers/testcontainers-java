package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.CreateNetworkResponse;
import org.testcontainers.controller.intents.CreateNetworkResult;

public class CreateNetworkDockerResult implements CreateNetworkResult {
    private final CreateNetworkResponse exec;

    public CreateNetworkDockerResult(CreateNetworkResponse exec) {
        this.exec = exec;
    }

    @Override
    public String getId() {
        return exec.getId();
    }
}

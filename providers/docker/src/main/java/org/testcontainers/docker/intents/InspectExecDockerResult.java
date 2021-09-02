package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.InspectExecResponse;
import org.testcontainers.controller.intents.InspectExecResult;

public class InspectExecDockerResult implements InspectExecResult {
    private final InspectExecResponse exec;

    public InspectExecDockerResult(InspectExecResponse exec) {
        this.exec = exec;
    }

    @Override
    public Integer getExitCode() {
        return exec.getExitCode();
    }
}

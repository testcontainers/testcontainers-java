package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import org.testcontainers.controller.intents.ExecCreateResult;

public class ExecCreateDockerResult implements ExecCreateResult {
    private final ExecCreateCmdResponse exec;

    public ExecCreateDockerResult(ExecCreateCmdResponse exec) {
        this.exec = exec;
    }

    @Override
    public String getId() {
        return exec.getId();
    }
}

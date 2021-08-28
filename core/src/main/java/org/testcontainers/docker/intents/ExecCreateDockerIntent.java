package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.ExecCreateCmd;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import org.testcontainers.controller.intents.ExecCreateIntent;
import org.testcontainers.controller.intents.ExecCreateResult;

public class ExecCreateDockerIntent implements ExecCreateIntent {
    private final ExecCreateCmd execCreateCmd;

    public ExecCreateDockerIntent(ExecCreateCmd execCreateCmd) {
        this.execCreateCmd = execCreateCmd;
    }

    @Override
    public ExecCreateIntent withAttachStdout(boolean attachStdout) {
        execCreateCmd.withAttachStdout(attachStdout);
        return this;
    }

    @Override
    public ExecCreateIntent withAttachStderr(boolean attachStderr) {
        execCreateCmd.withAttachStderr(attachStderr);
        return this;
    }

    @Override
    public ExecCreateIntent withCmd(String... command) {
        execCreateCmd.withCmd(command);
        return this;
    }

    @Override
    public ExecCreateResult exec() {
        return new ExecCreateDockerResult(execCreateCmd.exec());
    }
}

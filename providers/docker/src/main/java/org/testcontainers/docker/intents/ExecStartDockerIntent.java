package org.testcontainers.docker.intents;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecStartCmd;
import com.github.dockerjava.api.model.Frame;
import org.testcontainers.controller.intents.ExecStartIntent;

public class ExecStartDockerIntent implements ExecStartIntent {
    private final ExecStartCmd execStartCmd;

    public ExecStartDockerIntent(ExecStartCmd execStartCmd) {
        this.execStartCmd = execStartCmd;
    }

    @Override
    public <T extends ResultCallback<Frame>> T perform(T resultCallback) {
        return execStartCmd.exec(resultCallback);
    }
}

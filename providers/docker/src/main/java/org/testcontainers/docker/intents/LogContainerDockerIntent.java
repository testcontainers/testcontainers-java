package org.testcontainers.docker.intents;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.model.Frame;
import org.testcontainers.controller.intents.LogContainerIntent;

public class LogContainerDockerIntent implements LogContainerIntent {
    private final LogContainerCmd logContainerCmd;

    public LogContainerDockerIntent(LogContainerCmd logContainerCmd) {
        this.logContainerCmd = logContainerCmd;
    }

    @Override
    public LogContainerIntent withSince(int i) {
        logContainerCmd.withSince(i);
        return this;
    }

    @Override
    public LogContainerIntent withFollowStream(boolean followStream) {
        logContainerCmd.withFollowStream(followStream);
        return this;
    }

    @Override
    public LogContainerIntent withStdOut(boolean withStdOut) {
        logContainerCmd.withStdOut(withStdOut);
        return this;
    }

    @Override
    public LogContainerIntent withStdErr(boolean withStdErr) {
        logContainerCmd.withStdErr(withStdErr);
        return this;
    }

    @Override
    public <T extends ResultCallback<Frame>> T perform(T resultCallback) {
        return logContainerCmd.exec(resultCallback);
    }
}

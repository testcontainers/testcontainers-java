package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.WaitContainerCmd;
import org.testcontainers.controller.intents.WaitContainerCallback;
import org.testcontainers.controller.intents.WaitContainerIntent;

public class WaitContainerDockerIntent implements WaitContainerIntent {
    private final WaitContainerCmd waitContainerCmd;

    public WaitContainerDockerIntent(WaitContainerCmd waitContainerCmd) {
        this.waitContainerCmd = waitContainerCmd;
    }

    @Override
    public WaitContainerCallback start() {
        return new WaitContainerDockerCallback(waitContainerCmd.start());
    }
}

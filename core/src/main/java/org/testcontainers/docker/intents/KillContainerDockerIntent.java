package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.KillContainerCmd;
import org.testcontainers.controller.intents.KillContainerIntent;

public class KillContainerDockerIntent implements KillContainerIntent {
    private final KillContainerCmd killContainerCmd;

    public KillContainerDockerIntent(KillContainerCmd killContainerCmd) {
        this.killContainerCmd = killContainerCmd;
    }

    @Override
    public void perform() {
        killContainerCmd.exec();
    }
}

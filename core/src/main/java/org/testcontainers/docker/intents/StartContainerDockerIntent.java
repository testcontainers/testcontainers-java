package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.StartContainerCmd;
import org.testcontainers.controller.intents.StartContainerIntent;

public class StartContainerDockerIntent implements StartContainerIntent {

    private final StartContainerCmd startContainerCmd;

    public StartContainerDockerIntent(StartContainerCmd startContainerCmd) {
        this.startContainerCmd = startContainerCmd;
    }

    @Override
    public void perform() {
        startContainerCmd.exec();
    }
}

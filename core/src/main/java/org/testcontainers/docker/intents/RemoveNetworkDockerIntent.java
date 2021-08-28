package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.RemoveNetworkCmd;
import org.testcontainers.controller.intents.RemoveNetworkIntent;

public class RemoveNetworkDockerIntent implements RemoveNetworkIntent {
    private final RemoveNetworkCmd removeNetworkCmd;

    public RemoveNetworkDockerIntent(RemoveNetworkCmd removeNetworkCmd) {
        this.removeNetworkCmd = removeNetworkCmd;
    }

    @Override
    public void perform() {
        removeNetworkCmd.exec();
    }
}

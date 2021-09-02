package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.RemoveContainerCmd;
import org.testcontainers.controller.intents.RemoveContainerIntent;

public class RemoveContainerDockerIntent implements RemoveContainerIntent {
    private final RemoveContainerCmd removeContainerCmd;

    public RemoveContainerDockerIntent(RemoveContainerCmd removeContainerCmd) {
        this.removeContainerCmd = removeContainerCmd;
    }

    @Override
    public RemoveContainerIntent withRemoveVolumes(boolean removeVolumes) {
        removeContainerCmd.withRemoveVolumes(removeVolumes);
        return this;
    }

    @Override
    public RemoveContainerIntent withForce(boolean force) {
        removeContainerCmd.withForce(force);
        return this;
    }

    @Override
    public void perform() {
        removeContainerCmd.exec();
    }
}

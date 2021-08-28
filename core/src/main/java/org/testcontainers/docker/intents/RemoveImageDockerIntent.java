package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.RemoveImageCmd;
import org.testcontainers.controller.intents.RemoveImageIntent;

public class RemoveImageDockerIntent implements RemoveImageIntent {
    private final RemoveImageCmd removeImageCmd;

    public RemoveImageDockerIntent(RemoveImageCmd removeImageCmd) {
        this.removeImageCmd = removeImageCmd;
    }

    @Override
    public RemoveImageIntent withForce(boolean force) {
        removeImageCmd.withForce(force);
        return this;
    }

    @Override
    public void perform() {
        removeImageCmd.exec();
    }
}

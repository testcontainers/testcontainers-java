package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.InspectImageCmd;
import org.testcontainers.controller.intents.InspectImageIntent;
import org.testcontainers.controller.intents.InspectImageResult;

public class InspectImageDockerIntent implements InspectImageIntent {
    private final InspectImageCmd inspectImageCmd;

    public InspectImageDockerIntent(InspectImageCmd inspectImageCmd) {
        this.inspectImageCmd = inspectImageCmd;
    }

    @Override
    public InspectImageResult perform() {
        return new InspectImageDockerResult(inspectImageCmd.exec());
    }
}

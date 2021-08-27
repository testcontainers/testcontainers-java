package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.InspectContainerCmd;
import org.testcontainers.controller.intents.InspectContainerIntent;
import org.testcontainers.controller.intents.InspectContainerResult;

public class InspectContainerDockerIntent implements InspectContainerIntent {

    private final InspectContainerCmd inspectContainerCmd;

    public InspectContainerDockerIntent(InspectContainerCmd inspectContainerCmd) {
        this.inspectContainerCmd = inspectContainerCmd;
    }

    @Override
    public InspectContainerResult perform() {
        return new InspectContainerDockerResult(inspectContainerCmd.exec());
    }
}

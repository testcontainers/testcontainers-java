package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.InspectExecCmd;
import org.testcontainers.controller.intents.InspectExecIntent;
import org.testcontainers.controller.intents.InspectExecResult;

public class InspectExecDockerIntent implements InspectExecIntent {
    private final InspectExecCmd inspectExecCmd;

    public InspectExecDockerIntent(InspectExecCmd inspectExecCmd) {
        this.inspectExecCmd = inspectExecCmd;
    }

    @Override
    public InspectExecResult perform() {
        return new InspectExecDockerResult(inspectExecCmd.exec());
    }
}

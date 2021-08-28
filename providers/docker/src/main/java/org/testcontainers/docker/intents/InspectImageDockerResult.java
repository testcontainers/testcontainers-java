package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.InspectImageResponse;
import org.testcontainers.controller.intents.InspectImageResult;

import java.time.Instant;
import java.time.ZonedDateTime;

public class InspectImageDockerResult implements InspectImageResult {
    private final InspectImageResponse inspectImageResponse;

    public InspectImageDockerResult(InspectImageResponse inspectImageResponse) {
        this.inspectImageResponse = inspectImageResponse;
    }

    @Override
    public Instant getCreated() {
        return ZonedDateTime.parse(inspectImageResponse.getCreated()).toInstant();
    }
}

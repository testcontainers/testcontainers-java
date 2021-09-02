package org.testcontainers.controller.intents;

import org.testcontainers.controller.model.ImageConfig;

import java.time.Instant;

public interface InspectImageResult {
    Instant getCreated();

    ImageConfig getConfig();
}

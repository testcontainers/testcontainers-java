package org.testcontainers.controller.intents;

import com.github.dockerjava.api.command.InspectImageResponse;

public interface InspectImageIntent {
     InspectImageResult perform();
}

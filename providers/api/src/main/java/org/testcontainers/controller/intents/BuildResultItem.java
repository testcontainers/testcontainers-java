package org.testcontainers.controller.intents;

import org.testcontainers.controller.model.BuildErrorDetails;

public interface BuildResultItem {
    boolean isBuildSuccessIndicated();

    String getImageId();

    boolean isErrorIndicated();

    String getError();

    BuildErrorDetails getErrorDetail();

    String getStream();
}

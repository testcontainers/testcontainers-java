package org.testcontainers.docker.model;

import com.github.dockerjava.api.model.BuildResponseItem;
import org.testcontainers.controller.intents.BuildResultItem;
import org.testcontainers.controller.model.BuildErrorDetails;

public class DockerBuildResponseItem implements BuildResultItem {
    private final BuildResponseItem item;

    public DockerBuildResponseItem(BuildResponseItem item) {
        this.item = item;
    }

    @Override
    public boolean isBuildSuccessIndicated() {
        return item.isBuildSuccessIndicated();
    }

    @Override
    public String getImageId() {
        return item.getImageId();
    }

    @Override
    public boolean isErrorIndicated() {
        return item.isErrorIndicated();
    }

    @Override
    public String getError() {
        return item.getError();
    }

    @Override
    public BuildErrorDetails getErrorDetail() {
        return new DockerBuildErrorDetails(item.getErrorDetail());
    }

    @Override
    public String getStream() {
        return item.getStream();
    }
}

package org.testcontainers.providers.kubernetes.worker;

import org.testcontainers.controller.model.BuildErrorDetails;

public class KubernetesBuildResultItem implements org.testcontainers.controller.intents.BuildResultItem { // TODO: Move to package

    private final boolean isSuccess;
    private final String imageId;

    private KubernetesBuildResultItem(boolean isSuccess, String imageId) {
        this.isSuccess = isSuccess;
        this.imageId = imageId;
    }

    public static KubernetesBuildResultItem success(String imageId) {
        return new KubernetesBuildResultItem(true, imageId);
    }

    @Override
    public boolean isBuildSuccessIndicated() {
        return isSuccess;
    }

    @Override
    public String getImageId() {
        return imageId;
    }

    @Override
    public boolean isErrorIndicated() {
        return !isSuccess;
    }

    @Override
    public String getError() {
        return "";
    }

    @Override
    public BuildErrorDetails getErrorDetail() {
        return null; // TODO: Implement
    }

    @Override
    public String getStream() {
        return null;
    }
}

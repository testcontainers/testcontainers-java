package org.testcontainers.docker.model;

import com.github.dockerjava.api.model.ResponseItem;
import org.testcontainers.controller.model.BuildErrorDetails;

public class DockerBuildErrorDetails implements BuildErrorDetails {
    private final ResponseItem.ErrorDetail errorDetail;

    public DockerBuildErrorDetails(ResponseItem.ErrorDetail errorDetail) {
        this.errorDetail = errorDetail;
    }

    @Override
    public String getMessage() {
        return errorDetail.getMessage();
    }
}

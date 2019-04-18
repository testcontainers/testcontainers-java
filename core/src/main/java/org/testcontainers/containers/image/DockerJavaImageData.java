package org.testcontainers.containers.image;

import com.github.dockerjava.api.command.InspectImageResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Value;

@Value
public class DockerJavaImageData implements ImageData {

    InspectImageResponse inspectImageResponse;

    @Override
    public Instant getCreated() {
        return OffsetDateTime.parse(inspectImageResponse.getCreated()).toInstant();
    }

    @Override
    public String getId() {
        return inspectImageResponse.getId();
    }

    @Override
    public String getParentId() {
        return inspectImageResponse.getParent();
    }

    @Override
    public List<String> getRepoTags() {
        return inspectImageResponse.getRepoTags();
    }

    @Override
    public List<String> getRepoDigests() {
        return inspectImageResponse.getRepoDigests();
    }

    @Override
    public Long getSize() {
        return inspectImageResponse.getSize();
    }

    @Override
    public Long getVirtualSize() {
        return inspectImageResponse.getVirtualSize();
    }
}


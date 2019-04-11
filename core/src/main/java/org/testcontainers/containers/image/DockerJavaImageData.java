package org.testcontainers.containers.image;

import com.github.dockerjava.api.command.InspectImageResponse;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;
import lombok.Value;

@Data
@Value
public class DockerJavaImageData implements ImageData {

    private InspectImageResponse inspectImageResponse;

    public DockerJavaImageData(InspectImageResponse inspectImageData) {
        this.inspectImageResponse = inspectImageData;
    }

    @Override
    public Long getCreated() {
        return OffsetDateTime.parse(inspectImageResponse.getCreated()).toEpochSecond();
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


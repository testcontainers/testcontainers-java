package org.testcontainers.containers.image;

import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.Image;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor(access = AccessLevel.MODULE)
public class ImageData {

    public static ImageData from(Image image) {
        return new ImageData(image.getCreated(), image.getId(), image.getParentId(), Arrays.asList(image.getRepoTags()), new ArrayList<>(), image.getSize(), image.getVirtualSize());
    }

    public static ImageData from(InspectImageResponse imageResponse) {
        return new ImageData(OffsetDateTime.parse(imageResponse.getCreated()).toEpochSecond(), imageResponse.getId(), imageResponse.getParent(), imageResponse.getRepoTags(), imageResponse.getRepoDigests(), imageResponse.getSize(), imageResponse.getVirtualSize());
    }

    private final Long created;
    private final String id;
    private final String parentId;
    private final List<String> repoTags;
    private final List<String> repoDigests;
    private final Long size;
    private final Long virtualSize;


}

package org.testcontainers.images;

import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.Image;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.time.ZonedDateTime;

@Value
@Builder
public class ImageData {

    @NonNull
    Instant createdAt;

    static ImageData from(InspectImageResponse inspectImageResponse) {
        return ImageData.builder()
            .createdAt(ZonedDateTime.parse(inspectImageResponse.getCreated()).toInstant())
            .build();
    }

    static ImageData from(Image image) {
        return ImageData.builder()
            .createdAt(Instant.ofEpochMilli(image.getCreated()))
            .build();
    }
}

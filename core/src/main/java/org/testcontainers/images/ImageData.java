package org.testcontainers.images;

import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.model.Image;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.testcontainers.controller.intents.InspectImageResult;

import java.time.Instant;
import java.time.ZonedDateTime;

@Value
@Builder
public class ImageData {

    @NonNull
    Instant createdAt;

    // TODO: Remove
    @Deprecated
    static ImageData from(InspectImageResponse inspectImageResponse) {
        return ImageData.builder()
            .createdAt(ZonedDateTime.parse(inspectImageResponse.getCreated()).toInstant())
            .build();
    }

    static ImageData from(InspectImageResult inspectImageResult) {
        return ImageData.builder()
            .createdAt(inspectImageResult.getCreated())
            .build();
    }

    static ImageData from(Image image) {
        return ImageData.builder()
            .createdAt(Instant.ofEpochMilli(image.getCreated()))
            .build();
    }
}

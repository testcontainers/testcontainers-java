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
        final String created = inspectImageResponse.getCreated();
        final Instant createdInstant =
            ((created == null) || created.isEmpty()) ? Instant.EPOCH : ZonedDateTime.parse(created).toInstant();
        return ImageData.builder().createdAt(createdInstant).build();
    }

    static ImageData from(Image image) {
        return ImageData.builder().createdAt(Instant.ofEpochSecond(image.getCreated())).build();
    }
}

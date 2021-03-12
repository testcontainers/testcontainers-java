package org.testcontainers.images;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.Test;

import com.github.dockerjava.api.command.InspectImageResponse;

public class ImageDataTest {

    @Test
    public void shouldReadTimestampWithoutOffsetFromInspectImageResponse() {
        final String timestamp = "2020-07-27T18:23:31.365190246Z";
        final ImageData imageData = ImageData.from(new InspectImageResponse().withCreated(timestamp));
        assertThat(imageData.getCreatedAt()).isEqualTo(Instant.parse(timestamp));
    }

    @Test
    public void shouldReadTimestampWithOffsetFromInspectImageResponse() {
        final String timestamp = "2020-07-27T18:23:31.365190246+02:00";
        final ImageData imageData = ImageData.from(new InspectImageResponse().withCreated(timestamp));
        assertThat(imageData.getCreatedAt()).isEqualTo(Instant.parse("2020-07-27T16:23:31.365190246Z"));
    }

}

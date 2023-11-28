package org.testcontainers.images;

import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class AgeBasedPullPolicyTest {

    final DockerImageName imageName = DockerImageName.parse(UUID.randomUUID().toString());

    @Test
    public void shouldPull() {
        ImageData imageData = ImageData.builder().createdAt(Instant.now().minus(2, ChronoUnit.HOURS)).build();

        AgeBasedPullPolicy oneHour = new AgeBasedPullPolicy(Duration.of(1L, ChronoUnit.HOURS));
        assertThat(oneHour.shouldPullCached(imageName, imageData)).isTrue();

        AgeBasedPullPolicy fiveHours = new AgeBasedPullPolicy(Duration.of(5L, ChronoUnit.HOURS));
        assertThat(fiveHours.shouldPullCached(imageName, imageData)).isFalse();
    }
}

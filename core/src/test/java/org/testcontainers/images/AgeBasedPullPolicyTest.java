package org.testcontainers.images;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

public class AgeBasedPullPolicyTest {

    final DockerImageName imageName = DockerImageName.parse(UUID.randomUUID().toString());

    @Test
    public void shouldPull() {
        ImageData imageData = ImageData.builder()
            .createdAt(Instant.now().minus(2, ChronoUnit.HOURS))
            .build();

        AgeBasedPullPolicy oneHour = new AgeBasedPullPolicy(Duration.of(1L, ChronoUnit.HOURS));
        assertTrue(oneHour.shouldPullCached(imageName, imageData));

        AgeBasedPullPolicy fiveHours = new AgeBasedPullPolicy(Duration.of(5L, ChronoUnit.HOURS));
        assertFalse(fiveHours.shouldPullCached(imageName, imageData));
    }
}

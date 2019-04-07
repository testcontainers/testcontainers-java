package org.testcontainers.containers.image.pull.policy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.model.Image;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.testcontainers.containers.image.ImageData;

public class AgeBasedPullPolicyTest {

    private ImageData dockerImageData;

    @Before
    public void init() {
        Image dockerImage = Mockito.mock(Image.class);
        when(dockerImage.getCreated()).thenReturn(Instant.now().minus(2, ChronoUnit.HOURS).getEpochSecond());
        when(dockerImage.getRepoTags()).thenReturn(new String[]{});
        this.dockerImageData = ImageData.from(dockerImage);
    }

    @Test
    public void shouldPull() {
        ImagePullPolicy one_hour = new AgeBasedPullPolicy(Duration.of(1L, ChronoUnit.HOURS));
        assertTrue(one_hour.shouldPull(dockerImageData));
        ImagePullPolicy five_hours = new AgeBasedPullPolicy(Duration.of(5L, ChronoUnit.HOURS));
        assertFalse(five_hours.shouldPull(dockerImageData));
    }
}

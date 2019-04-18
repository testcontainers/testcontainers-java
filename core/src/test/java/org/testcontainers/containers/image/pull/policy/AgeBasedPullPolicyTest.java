package org.testcontainers.containers.image.pull.policy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.containers.image.ImageData;

public class AgeBasedPullPolicyTest {

    @Mock
    private ImageData dockerImageData;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(dockerImageData.getCreated()).thenReturn(Instant.now().minus(2, ChronoUnit.HOURS));
    }

    @Test
    public void shouldPull() {
        ImagePullPolicy one_hour = new AgeBasedPullPolicy(Duration.of(1L, ChronoUnit.HOURS));
        assertTrue(one_hour.shouldPull(dockerImageData));
        ImagePullPolicy five_hours = new AgeBasedPullPolicy(Duration.of(5L, ChronoUnit.HOURS));
        assertFalse(five_hours.shouldPull(dockerImageData));
    }
}

package org.testcontainers.containers.image.pull.policy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.containers.image.ImageData;

public class AgeBasedPullPolicyTest {

    @Mock
    private ImageData dockerImage;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(dockerImage.getCreated()).thenReturn(Instant.now().minus(2, ChronoUnit.HOURS).getEpochSecond());
        when(dockerImage.getRepoTags()).thenReturn(Collections.emptyList());
    }

    @Test
    public void shouldPull() {
        ImagePullPolicy one_hour = new AgeBasedPullPolicy(1L, TimeUnit.HOURS);
        assertTrue(one_hour.shouldPull(dockerImage));
        ImagePullPolicy five_hours = new AgeBasedPullPolicy(5L, TimeUnit.HOURS);
        assertFalse(five_hours.shouldPull(dockerImage));
    }
}

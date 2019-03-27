package org.testcontainers.containers.image.pull.policy;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

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
        when(dockerImage.getCreated()).thenReturn(System.currentTimeMillis() / 1000 - 2 * 3_600L);
        when(dockerImage.getRepoTags()).thenReturn(Collections.emptyList());
    }

    @Test
    public void shouldPull() {
        ImagePullPolicy ageBased = new AgeBasedPullPolicy(1L, TimeUnit.HOURS);
        assertTrue(ageBased.shouldPull(dockerImage));
    }
}

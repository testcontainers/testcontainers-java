package org.testcontainers.containers.image.pull.policy;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.github.dockerjava.api.model.Image;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AgeBasedPullPolicyTest {

    @Mock
    private Image dockerImage;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(dockerImage.getCreated()).thenReturn(System.currentTimeMillis() / 1000 - 2 * 3_600L);
        when(dockerImage.getRepoTags()).thenReturn(new String[]{});
    }

    @Test
    public void shouldPull() {
        ImagePullPolicy ageBased = new AgeBasedPullPolicy(1L, TimeUnit.HOURS);
        assertTrue(ageBased.shouldPull(dockerImage));
    }
}

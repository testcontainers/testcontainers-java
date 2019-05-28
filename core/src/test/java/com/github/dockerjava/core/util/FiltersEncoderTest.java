package com.github.dockerjava.core.util;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import org.junit.Test;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.DockerClientFactory;

import java.util.List;
import java.util.UUID;

/**
 * This test checks that our custom monkey-patched version works
 * and does not throw {@link ClassNotFoundException}.
 */
public class FiltersEncoderTest {

    @Test
    public void filtersShouldWork() throws Exception {
        DockerClient client = DockerClientFactory.instance().client();

        List<Image> images = client.listImagesCmd()
            .withLabelFilter("com.example=" + UUID.randomUUID().toString())
            .exec();

        VisibleAssertions.assertTrue("List is empty", images.isEmpty());
    }
}

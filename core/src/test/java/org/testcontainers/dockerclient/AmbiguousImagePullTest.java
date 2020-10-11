package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

public class AmbiguousImagePullTest {

    @Test(timeout = 30_000)
    public void testNotUsingParse() {
        DockerClient client = DockerClientFactory.instance().client();
        List<Image> alpineImages = client.listImagesCmd()
            .withImageNameFilter("alpine:latest")
            .exec();
        for (Image alpineImage : alpineImages) {
            client.removeImageCmd(alpineImage.getId()).exec();
        }

        try (
            final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("alpine"))
                .withCommand("/bin/sh", "-c", "sleep 0")
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        ) {
            container.start();
            // do nothing other than start and stop
        }
    }
}

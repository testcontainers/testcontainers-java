package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Image;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

public class AmbiguousImagePullTest {

    @Test(timeout = 30_000)
    public void testNotUsingParse() {
        DockerClient client = DockerClientFactory.instance().client();
        List<Image> alpineImages = client.listImagesCmd()
            .withImageNameFilter("testcontainers/helloworld:latest")
            .exec();
        for (Image alpineImage : alpineImages) {
            client.removeImageCmd(alpineImage.getId()).exec();
        }

        try (
            final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("testcontainers/helloworld"))
                .withExposedPorts(8080)
        ) {
            container.start();
            // do nothing other than start and stop
        }
    }
}

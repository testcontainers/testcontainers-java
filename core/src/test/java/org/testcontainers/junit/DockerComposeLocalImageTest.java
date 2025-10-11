package org.testcontainers.junit;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

class DockerComposeLocalImageTest {

    @Test
    void usesLocalImageEvenWhenPullFails() throws InterruptedException {
        tagImage();

        try (
            DockerComposeContainer<?> composeContainer = new DockerComposeContainer<>(
                DockerImageName.parse("docker/compose:1.29.2"),
                new File("src/test/resources/local-compose-test.yml")
            )
                .withExposedService("redis", 6379)
        ) {
            composeContainer.start();
        }
    }

    private void tagImage() throws InterruptedException {
        DockerClient client = DockerClientFactory.instance().client();
        client.pullImageCmd("redis:6-alpine").exec(new PullImageResultCallback()).awaitCompletion();
        client.tagImageCmd("redis:6-alpine", "redis-local", "latest").exec();
    }
}

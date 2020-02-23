package org.testcontainers.junit;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;

public class DockerComposeLocalImageTest {

    @Test
    public void usesLocalImageEvenWhenPullFails() throws InterruptedException {
        tagImage("redis:4.0.10", "redis-local", "latest");

        DockerComposeContainer composeContainer = new DockerComposeContainer(new File("src/test/resources/local-compose-test.yml"))
            .withExposedService("redis", 6379);
        composeContainer.start();
    }

    private void tagImage(String sourceImage, String targetImage, String targetTag) throws InterruptedException {
        DockerClient client = DockerClientFactory.instance().client();
        client.pullImageCmd(sourceImage).exec(new PullImageResultCallback()).awaitCompletion();
        client.tagImageCmd(sourceImage, targetImage, targetTag).exec();
    }
}

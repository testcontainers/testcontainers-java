package org.testcontainers.jib;

import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerClient;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.RegistryException;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutionException;

public class JibTest {

    @Test
    public void test()
        throws CacheDirectoryCreationException, IOException, ExecutionException, InterruptedException, RegistryException, InvalidImageReferenceException {
        DockerDaemonImage dockerDaemonImage = DockerDaemonImage.named("busybox");
        DockerClient dockerClient = new JibDockerClient();
        Containerizer containerizer = Containerizer.to(dockerClient, dockerDaemonImage);
        JibContainer jibContainer = Jib
            .from(dockerClient, dockerDaemonImage)
            .setEntrypoint("echo", "Hello World")
            .containerize(containerizer);
        System.out.printf("digest %s%n", jibContainer.getDigest().toString());
        System.out.printf("imageId %s%n", jibContainer.getImageId().toString());

        try (
            GenericContainer<?> busybox = new GenericContainer<>(jibContainer.getTargetImage().toStringWithQualifier())
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(3)))
        ) {
            busybox.start();
        }
    }
}

package org.testcontainers.containers;

import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.RegistryException;
import org.junit.Test;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class JibTest {

    @Test
    public void test()
        throws CacheDirectoryCreationException, IOException, ExecutionException, InterruptedException, RegistryException, InvalidImageReferenceException {
        String image = "busybox:1.35";

        new RemoteDockerImage(DockerImageName.parse(image)).get();

        JibContainer jibContainer = Jib
            .from(image)
            .setEntrypoint("echo", "Hello World")
            .containerize(Containerizer.to(DockerDaemonImage.named("tmp-image-" + UUID.randomUUID())));

        try (
            GenericContainer<?> busybox = new GenericContainer<>(jibContainer.getTargetImage().toString())
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(3)))
        ) {
            busybox.start();
            String logs = busybox.getLogs(OutputFrame.OutputType.STDOUT);
            assertThat(logs).contains("Hello World");
        }
    }
}

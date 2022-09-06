package org.testcontainers.containers;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerClient;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
import org.junit.Test;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class JibTest {

    @Test
    public void testJibFromWithString() throws Exception {
        String image = "busybox:1.35";

        DockerClient dockerClient = JibDockerClient.instance();

        JibContainer jibContainer = Jib
            .from(image)
            .setEntrypoint("echo", "Hello World")
            .containerize(Containerizer.to(dockerClient, DockerDaemonImage.named("jib-hello-world")));

        try (
            GenericContainer<?> busybox = new GenericContainer<>(new JibImage(jibContainer))
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(3)))
        ) {
            busybox.start();
            String logs = busybox.getLogs(OutputFrame.OutputType.STDOUT);
            assertThat(logs).contains("Hello World");
        }
    }

    @Test
    public void testJibFromWithDockerDaemonImage() throws Exception {
        String image = "busybox:1.35";

        DockerClient dockerClient = JibDockerClient.instance();

        JibContainer jibContainer = Jib
            .from(dockerClient, DockerDaemonImage.named(image))
            .setEntrypoint("echo", "Hello World")
            .containerize(Containerizer.to(DockerDaemonImage.named("jib-hello-world")));

        try (
            GenericContainer<?> busybox = new GenericContainer<>(new JibImage(jibContainer))
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(3)))
        ) {
            busybox.start();
            String logs = busybox.getLogs(OutputFrame.OutputType.STDOUT);
            assertThat(logs).contains("Hello World");
        }
    }
}

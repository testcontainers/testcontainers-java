package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectImageResponse;
import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.OutputFrame.OutputType;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.jib.JibImage;

import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("Jib doesn't work with latest Docker version provided by GH Actions. Jib should be updated")
public class JibTest {

    @Test
    public void buildImage() {
        try (
            // jibContainerUsage {
            GenericContainer<?> busybox = new GenericContainer<>(
                new JibImage(
                    "busybox:1.35",
                    jibContainerBuilder -> {
                        return jibContainerBuilder.setEntrypoint("echo", "Hello World");
                    }
                )
            )
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(3)))
            // }
        ) {
            busybox.start();
            String logs = busybox.getLogs(OutputType.STDOUT);
            assertThat(logs).contains("Hello World");
        }
    }

    @Test
    public void standardLabelsAreAddedWhenUsingJibSetLabels() {
        try (
            GenericContainer<?> busybox = new GenericContainer<>(
                new JibImage(
                    "busybox:1.35",
                    jibContainerBuilder -> {
                        return jibContainerBuilder
                            .setEntrypoint("echo", "Hello World")
                            .setLabels(Collections.singletonMap("foo", "bar"));
                    }
                )
            )
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(3)))
        ) {
            busybox.start();
            assertImageLabels(busybox);
        }
    }

    @Test
    public void standardLabelsAreAddedWhenUsingJibAddLabel() {
        try (
            GenericContainer<?> busybox = new GenericContainer<>(
                new JibImage(
                    "busybox:1.35",
                    jibContainerBuilder -> {
                        return jibContainerBuilder.setEntrypoint("echo", "Hello World").addLabel("foo", "bar");
                    }
                )
            )
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(3)))
        ) {
            busybox.start();
            assertImageLabels(busybox);
        }
    }

    private static void assertImageLabels(GenericContainer<?> busybox) {
        String image = busybox.getContainerInfo().getConfig().getImage();
        InspectImageResponse imageResponse = DockerClientFactory.lazyClient().inspectImageCmd(image).exec();
        assertThat(imageResponse.getConfig().getLabels())
            .containsEntry("foo", "bar")
            .containsKeys(
                "org.testcontainers",
                "org.testcontainers.sessionId",
                "org.testcontainers.lang",
                "org.testcontainers.version"
            );
    }
}

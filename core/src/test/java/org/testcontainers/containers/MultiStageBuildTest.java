package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiStageBuildTest {

    @Test
    public void testDockerMultistageBuild() throws IOException, InterruptedException {
        try (
            GenericContainer<?> container = new GenericContainer<>(
                new ImageFromDockerfile()
                    .withDockerfile(Paths.get("src/test/resources/Dockerfile-multistage"))
                    .withTarget("builder")
            )
                .withCommand("/bin/sh", "-c", "sleep 10")
        ) {
            container.start();
            assertThat(container.execInContainer("pwd").getStdout()).contains("/my-files");
            assertThat(container.execInContainer("ls").getStdout()).contains("hello.txt");
        }
    }

    @Test
    public void shouldBuildMultistageBuildWithBuildImageCmdModifier() throws IOException, InterruptedException {
        try (
            GenericContainer<?> container = new GenericContainer<>(
                new ImageFromDockerfile()
                    .withDockerfile(Paths.get("src/test/resources/Dockerfile-multistage"))
                    .withBuildImageCmdModifier(cmd -> cmd.withTarget("builder"))
            )
                .withCommand("/bin/sh", "-c", "sleep 10")
        ) {
            container.start();
            assertThat(container.execInContainer("pwd").getStdout()).contains("/my-files");
            assertThat(container.execInContainer("ls").getStdout()).contains("hello.txt");
        }
    }
}

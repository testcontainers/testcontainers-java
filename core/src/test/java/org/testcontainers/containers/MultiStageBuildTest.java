package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class MultiStageBuildTest {

    @Test
    void testDockerMultistageBuild() throws IOException, InterruptedException {
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
    void shouldBuildMultistageBuildWithBuildImageCmdModifier() throws IOException, InterruptedException {
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

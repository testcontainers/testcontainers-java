package org.testcontainers.images.builder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DockerfileBuildTest {

    static final Path RESOURCE_PATH = Paths.get("src/test/resources/dockerfile-build-test");

    public static Stream<Arguments> parameters() {
        Map<String, String> buildArgs = new HashMap<>(4);
        buildArgs.put("BUILD_IMAGE", "alpine:3.16");
        buildArgs.put("BASE_IMAGE", "alpine");
        buildArgs.put("BASE_IMAGE_TAG", "3.12");
        buildArgs.put("UNUSED", "ignored");

        //noinspection deprecation
        return Stream.of(
            // Dockerfile build without explicit per-file inclusion
            Arguments.of(
                "test1234",
                // spotless:off
                // docsShowRecursiveFileInclusion {
                new ImageFromDockerfile()
                    .withFileFromPath(".", RESOURCE_PATH)),
                // }
                // spotless:on
            // Dockerfile build using a non-standard Dockerfile
            Arguments.of(
                "test4567",
                new ImageFromDockerfile().withFileFromPath(".", RESOURCE_PATH).withDockerfilePath("./Dockerfile-alt")
            ),
            // Dockerfile build using withBuildArg()
            Arguments.of(
                "test7890",
                new ImageFromDockerfile()
                    .withFileFromPath(".", RESOURCE_PATH)
                    .withDockerfilePath("./Dockerfile-buildarg")
                    .withBuildArg("CUSTOM_ARG", "test7890")
            ),
            // Dockerfile build using withBuildArgs() with build args in FROM statement
            Arguments.of(
                "test1234",
                new ImageFromDockerfile()
                    .withFileFromPath(".", RESOURCE_PATH)
                    .withDockerfile(RESOURCE_PATH.resolve("Dockerfile-from-buildarg"))
                    .withBuildArgs(buildArgs)
            ),
            // Dockerfile build using withDockerfile(File)
            Arguments.of(
                "test4567",
                new ImageFromDockerfile()
                    .withFileFromPath(".", RESOURCE_PATH)
                    .withDockerfile(RESOURCE_PATH.resolve("Dockerfile-alt"))
            )
        );
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void performTest(String expectedFileContent, ImageFromDockerfile image) {
        try (
            final GenericContainer<?> container = new GenericContainer<>(image)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
                .withCommand("cat", "/test.txt")
        ) {
            container.start();

            final String logs = container.getLogs();
            assertThat(logs)
                .as("expected file content indicates that dockerfile build steps have been run")
                .contains(expectedFileContent);
        }
    }
}

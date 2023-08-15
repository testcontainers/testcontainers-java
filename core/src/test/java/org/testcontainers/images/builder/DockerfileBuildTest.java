package org.testcontainers.images.builder;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerfileBuildTest {

    static final Path RESOURCE_PATH = Paths.get("src/test/resources/dockerfile-build-test");

    public static Stream<Arguments> provideExpectedFileContentAndImage() {
        return Stream.of(
            Arguments.of("test1234", new ImageFromDockerfile().withFileFromPath(".", RESOURCE_PATH)),
            Arguments.of(
                "test4567",
                new ImageFromDockerfile().withFileFromPath(".", RESOURCE_PATH).withDockerfilePath("./Dockerfile-alt")
            ),
            Arguments.of(
                "test7890",
                new ImageFromDockerfile()
                    .withFileFromPath(".", RESOURCE_PATH)
                    .withDockerfilePath("./Dockerfile-buildarg")
                    .withBuildArg("CUSTOM_ARG", "test7890")
            ),
            Arguments.of(
                "test4567",
                new ImageFromDockerfile()
                    .withFileFromPath(".", RESOURCE_PATH)
                    .withDockerfile(RESOURCE_PATH.resolve("Dockerfile-alt"))
            )
        );
    }

    @ParameterizedTest
    @MethodSource("provideExpectedFileContentAndImage")
    public void performTest(String expectedFileContent, ImageFromDockerfile image) {
        try (
            final GenericContainer container = new GenericContainer(image)
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

package org.testcontainers.images.builder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ParameterizedClass
@MethodSource("parameters")
public class DockerfileBuildTest {

    static final Path RESOURCE_PATH = Paths.get("src/test/resources/dockerfile-build-test");

    public String expectedFileContent;

    public ImageFromDockerfile image;

    public static Object[][] parameters() {
        Map<String, String> buildArgs = new HashMap<>(4);
        buildArgs.put("BUILD_IMAGE", "alpine:3.16");
        buildArgs.put("BASE_IMAGE", "alpine");
        buildArgs.put("BASE_IMAGE_TAG", "3.12");
        buildArgs.put("UNUSED", "ignored");

        //noinspection deprecation
        return new Object[][] {
            // Dockerfile build without explicit per-file inclusion
            new Object[] {
                "test1234",
                // spotless:off
                // docsShowRecursiveFileInclusion {
                new ImageFromDockerfile()
                    .withFileFromPath(".", RESOURCE_PATH),
                // }
                // spotless:on
            },
            // Dockerfile build using a non-standard Dockerfile
            new Object[] {
                "test4567",
                new ImageFromDockerfile().withFileFromPath(".", RESOURCE_PATH).withDockerfilePath("./Dockerfile-alt"),
            },
            // Dockerfile build using withBuildArg()
            new Object[] {
                "test7890",
                new ImageFromDockerfile()
                    .withFileFromPath(".", RESOURCE_PATH)
                    .withDockerfilePath("./Dockerfile-buildarg")
                    .withBuildArg("CUSTOM_ARG", "test7890"),
            },
            // Dockerfile build using withBuildArgs() with build args in FROM statement
            new Object[] {
                "test1234",
                new ImageFromDockerfile()
                    .withFileFromPath(".", RESOURCE_PATH)
                    .withDockerfile(RESOURCE_PATH.resolve("Dockerfile-from-buildarg"))
                    .withBuildArgs(buildArgs),
            },
            // Dockerfile build using withDockerfile(File)
            new Object[] {
                "test4567",
                new ImageFromDockerfile()
                    .withFileFromPath(".", RESOURCE_PATH)
                    .withDockerfile(RESOURCE_PATH.resolve("Dockerfile-alt")),
            },
        };
    }

    public DockerfileBuildTest(String expectedFileContent, ImageFromDockerfile image) {
        this.expectedFileContent = expectedFileContent;
        this.image = image;
    }

    @Test
    public void performTest() {
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

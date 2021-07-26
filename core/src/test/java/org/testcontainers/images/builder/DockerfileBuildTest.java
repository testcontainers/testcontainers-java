package org.testcontainers.images.builder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

@RunWith(Parameterized.class)
public class DockerfileBuildTest {

    static final Path RESOURCE_PATH = Paths.get("src/test/resources/dockerfile-build-test");

    public String expectedFileContent;
    public ImageFromDockerfile image;

    @Parameterized.Parameters
    public static Object[][] parameters() {
        return new Object[][]{
            // Dockerfile build without explicit per-file inclusion
            new Object[]{"test1234",
                // docsShowRecursiveFileInclusion {
                new ImageFromDockerfile()
                    .withFileFromPath(".", RESOURCE_PATH)
                // }
            },

            // Dockerfile build using a non-standard Dockerfile
            new Object[]{"test4567",
                new ImageFromDockerfile()
                    .withFileFromPath(".", RESOURCE_PATH)
                    .withDockerfilePath("./Dockerfile-alt")
            },

            // Dockerfile build using build args
            new Object[]{"test7890",
                new ImageFromDockerfile()
                    .withFileFromPath(".", RESOURCE_PATH)
                    .withDockerfilePath("./Dockerfile-buildarg")
                    .withBuildArg("CUSTOM_ARG", "test7890")
            },
            
           // Dockerfile build using withDockerfile(File)
            new Object[]{"test4567",
                new ImageFromDockerfile()
                    .withFileFromPath(".", RESOURCE_PATH)
                    .withDockerfile(RESOURCE_PATH.resolve("Dockerfile-alt"))
            },
        };
    }

    public DockerfileBuildTest(String expectedFileContent, ImageFromDockerfile image) {
        this.expectedFileContent = expectedFileContent;
        this.image = image;
    }

    @Test
    public void performTest() {
        try (final GenericContainer container = new GenericContainer(image)
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
            .withCommand("cat", "/test.txt")) {
            container.start();

            final String logs = container.getLogs();
            assertTrue("expected file content indicates that dockerfile build steps have been run", logs.contains(expectedFileContent));
        }
    }
    
}

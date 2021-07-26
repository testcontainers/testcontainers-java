package org.testcontainers.images.builder;

import com.github.dockerjava.api.exception.DockerClientException;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.rnorth.visibleassertions.VisibleAssertions.fail;

public class DockerignoreTest {

    private static final Path INVALID_DOCKERIGNORE_PATH = Paths.get("src/test/resources/dockerfile-build-invalid");

    @Test
    public void testInvalidDockerignore() throws Exception {
        try {
            new ImageFromDockerfile()
                .withFileFromPath(".", INVALID_DOCKERIGNORE_PATH)
                .withDockerfile(INVALID_DOCKERIGNORE_PATH.resolve("Dockerfile"))
                .get();
            fail("Should not be able to build an image with an invalid .dockerignore file");
        }
        catch (DockerClientException e) {
            if (!e.getMessage().contains("Invalid pattern"))
                throw e;
        }
    }

    @SuppressWarnings("resource")
    @Test
    public void testValidDockerignore() throws Exception {
        ImageFromDockerfile img = new ImageFromDockerfile()
                .withFileFromPath(".", DockerfileBuildTest.RESOURCE_PATH)
                .withDockerfile(DockerfileBuildTest.RESOURCE_PATH.resolve("Dockerfile-currentdir"));
        try(
            final GenericContainer<?> container = new GenericContainer(DockerImageName.parse(img.get()))
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
            .withCommand("ls", "/")
        ) {

            container.start();

            final String logs = container.getLogs();
            assertTrue("Files in the container indicated the .dockerignore was not applied. Output was: " + logs,
                    logs.contains("should_not_be_ignored.txt"));
            assertTrue("Files in the container indicated the .dockerignore was not applied. Output was: " + logs,
                    !logs.contains("should_be_ignored.txt"));
        }
    }

}

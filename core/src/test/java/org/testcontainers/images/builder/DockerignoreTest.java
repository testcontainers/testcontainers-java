package org.testcontainers.images.builder;

import static org.rnorth.visibleassertions.VisibleAssertions.fail;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.github.dockerjava.api.exception.DockerClientException;

public class DockerignoreTest {
    
    private static final Path INVALID_DOCKERIGNORE_PATH = Paths.get("src/test/resources/dockerfile-build-invalid");
    
    @Test
    public void testDockerignoreWithFile() throws Exception {
        try {
            new ImageFromDockerfile()
                .withFileFromPath(".", INVALID_DOCKERIGNORE_PATH)
                .withDockerfile(new File("src/test/resources/dockerfile-build-invalid/Dockerfile"))
                .get();
            fail("Should not be able to build an image with an invalid .dockerignore file");
        } catch(DockerClientException e) {
            if (!e.getMessage().contains("Invalid pattern"))
                throw e;
        }
    }
    
    @Test
    public void testDockerignoreWithString() throws Exception {
        try {
            new ImageFromDockerfile()
                .withFileFromPath(".", INVALID_DOCKERIGNORE_PATH)
                .withDockerfile("src/test/resources/dockerfile-build-invalid/Dockerfile")
                .get();
            fail("Should not be able to build an image with an invalid .dockerignore file");
        } catch(DockerClientException e) {
            if (!e.getMessage().contains("Invalid pattern"))
                throw e;
        }
    }

}

package org.testcontainers.junit;

import com.github.dockerjava.api.command.BuildImageCmd;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.util.Arrays;
import java.util.List;

import static org.rnorth.visibleassertions.VisibleAssertions.pass;

public class DockerfileTest {

    @Test
    public void simpleDockerfileWorks() {
        ImageFromDockerfile image = new ImageFromDockerfile()
                .withFileFromString("folder/someFile.txt", "hello")
                .withFileFromClasspath("test.txt", "mappable-resource/test-resource.txt")
                .withFileFromClasspath("Dockerfile", "mappable-dockerfile/Dockerfile");

        verifyImage(image);
    }

    @Test
    public void customizableImage() {
        ImageFromDockerfile image = new ImageFromDockerfile() {
            @Override
            protected void configure(BuildImageCmd buildImageCmd) {
                super.configure(buildImageCmd);

                List<String> dockerfile = Arrays.asList(
                        "FROM alpine:3.2",
                        "RUN echo 'hello from Docker build process'",
                        "CMD yes"
                );
                withFileFromString("Dockerfile", String.join("\n", dockerfile));

                buildImageCmd.withNoCache();
            }
        };

        verifyImage(image);
    }

    @Test
    public void dockerfileBuilderWorks() {
        ImageFromDockerfile image = new ImageFromDockerfile()
                .withFileFromClasspath("test.txt", "mappable-resource/test-resource.txt")
                .withFileFromString("folder/someFile.txt", "hello")
                .withDockerfileFromBuilder(builder -> builder
                        .from("alpine:3.2")
                        .workDir("/app")
                        .add("test.txt", "test file.txt")
                        .run("ls", "-la", "/app/test file.txt")
                        .copy("folder/someFile.txt", "/someFile.txt")
                        .expose(80, 8080)
                        .cmd("while true; do cat /someFile.txt | nc -l -p 80; done")
                );

        verifyImage(image);
    }

    protected void verifyImage(ImageFromDockerfile image) {
        GenericContainer container = new GenericContainer(image);

        try {
            container.start();

            pass("Should start from Dockerfile");
        } finally {
            container.stop();
        }
    }
}

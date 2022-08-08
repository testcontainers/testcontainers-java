package org.testcontainers.utility;

import org.assertj.core.api.Condition;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectoryTarResourceTest {

    @Test
    public void simpleRecursiveFileTest() {
        // 'src' is expected to be the project base directory, so all source code/resources should be copied in
        File directory = new File("src");

        GenericContainer container = new GenericContainer(
            new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder -> {
                    builder
                        .from("alpine:3.16")
                        .copy("/tmp/foo", "/foo")
                        .cmd("cat /foo/test/resources/test-recursive-file.txt")
                        .build();
                })
                .withFileFromFile("/tmp/foo", directory)
        )
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy());

        container.start();

        final String results = container.getLogs();

        assertThat(results)
            .as("The container has a file that was copied in via a recursive copy")
            .contains("Used for DirectoryTarResourceTest");
    }

    @Test
    public void simpleRecursiveFileWithPermissionTest() {
        try (
            GenericContainer container = new GenericContainer(
                new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> {
                        builder
                            .from("alpine:3.16") //
                            .copy("/tmp/foo", "/foo")
                            .cmd("ls", "-al", "/")
                            .build();
                    })
                    .withFileFromFile("/tmp/foo", new File("/mappable-resource/test-resource.txt"), 0754)
            )
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        ) {
            container.start();
            String listing = container.getLogs();

            Predicate<String> condition = s -> s.contains("-rwxr-xr--") && s.contains("foo");
            assertThat(listing.split("\\n"))
                .as("Listing shows that file is copied with mode requested.")
                .haveAtLeastOne(new Condition<>(condition, "File not found in listing"));
        }
    }

    @Test
    public void simpleRecursiveClasspathResourceTest() {
        // This test combines the copying of classpath resources from JAR files with the recursive TAR approach, to allow JARed classpath resources to be copied in to an image

        try (
            GenericContainer container = new GenericContainer(
                new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> {
                        builder
                            .from("alpine:3.16") //
                            .copy("/tmp/foo", "/foo")
                            .cmd("ls -lRt /foo")
                            .build();
                    })
                    .withFileFromClasspath("/tmp/foo", "/recursive/dir")
            ) // here we use /org/junit as a directory that really should exist on the classpath
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        ) {
            container.start();

            final String results = container.getLogs();

            // ExternalResource.class is known to exist in a subdirectory of /org/junit so should be successfully copied in
            assertThat(results)
                .as("The container has a file that was copied in via a recursive copy from a JAR resource")
                .contains("content.txt");
        }
    }
}

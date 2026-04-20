package org.testcontainers.utility;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class DirectoryTarResourceTest {

    @Test
    void simpleRecursiveFileTest() {
        // 'src' is expected to be the project base directory, so all source code/resources should be copied in
        File directory = new File("src");

        try (
            GenericContainer container = new GenericContainer(
                new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> {
                        builder
                            .from("alpine:3.17")
                            .copy("/tmp/foo", "/foo")
                            .cmd("cat /foo/test/resources/test-recursive-file.txt")
                            .build();
                    })
                    .withFileFromFile("/tmp/foo", directory)
            )
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        ) {
            container.start();

            final String results = container.getLogs();

            assertThat(results)
                .as("The container has a file that was copied in via a recursive copy")
                .contains("Used for DirectoryTarResourceTest");
        }
    }

    @Test
    void simpleRecursiveFileWithPermissionTest() {
        try (
            GenericContainer container = new GenericContainer(
                new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> {
                        builder
                            .from("alpine:3.17") //
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
    void simpleRecursiveClasspathResourceTest() {
        // This test combines the copying of classpath resources from JAR files with the recursive TAR approach, to allow JARed classpath resources to be copied in to an image

        try (
            GenericContainer container = new GenericContainer(
                new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> {
                        builder
                            .from("alpine:3.17") //
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

    @Test
    public void transferFileDockerDaemon() {
        final File theFile = new File("src/test/resources/mappable-resource/test-resource.txt");
        try (
            GenericContainer container = new GenericContainer(
                new ImageFromDockerfile()
                    .withDockerfileFromBuilder(builder -> {
                        builder.from("alpine:3.3").copy(".", "/foo/").cmd("ls", "-lapR", "/foo").build();
                    })
                    .withFileFromFile("bar1", theFile)
                    .withFileFromFile("./bar2", theFile)
                    .withFileFromFile("../bar3", theFile)
                    .withFileFromFile(".bar4", theFile)
                    .withFileFromFile("..bar5", theFile)
                    .withFileFromFile("xxx/../bar6", theFile)
                    .withFileFromFile("x7/./bar7", theFile)
                    .withFileFromFile("x8/././bar8", theFile)
                    .withFileFromFile("x9/../../bar9", theFile)
            )
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        ) {
            container.start();

            final List<String> logLines = Arrays.asList(container.getLogs().split("\\n"));
            assertThat(logLines.stream().filter(StringUtils::isEmpty).count())
                .describedAs("Three groups of dirs")
                .isEqualTo(2);

            final LsOutput lsOutput = LsOutput.parse(logLines);

            assertThat(lsOutput.parentDir).satisfiesOnlyOnce(endsWith(" bar1"));
            assertThat(lsOutput.parentDir).satisfiesOnlyOnce(endsWith(" bar2"));
            assertThat(lsOutput.parentDir).satisfiesOnlyOnce(endsWith(" bar3"));
            assertThat(lsOutput.parentDir).satisfiesOnlyOnce(endsWith(" .bar4"));
            assertThat(lsOutput.parentDir).satisfiesOnlyOnce(endsWith(" ..bar5"));
            assertThat(lsOutput.parentDir).satisfiesOnlyOnce(endsWith(" bar6"));
            assertThat(lsOutput.parentDir).satisfiesOnlyOnce(endsWith(" x7/"));
            assertThat(lsOutput.parentDir).satisfiesOnlyOnce(endsWith(" x8/"));
            assertThat(lsOutput.parentDir).satisfiesOnlyOnce(endsWith(" bar9"));
            assertThat(lsOutput.subDir1).satisfiesOnlyOnce(endsWith(" bar7"));
            assertThat(lsOutput.subDir2).satisfiesOnlyOnce(endsWith(" bar8"));
        }
    }

    private static class LsOutput {

        private final List<String> parentDir;

        private final List<String> subDir1;

        private final List<String> subDir2;

        private static LsOutput parse(List<String> logLines) {
            List<String> parentDir = null;
            List<String> subDir1 = null;
            int start = 0;
            for (int i = 0; i < logLines.size(); i++) {
                if (logLines.get(i).isEmpty()) {
                    if (parentDir == null) {
                        parentDir = new ArrayList<>(logLines.subList(start, i));
                        start = i;
                    } else if (subDir1 == null) {
                        subDir1 = new ArrayList<>(logLines.subList(start, i));
                        start = i;
                    }
                }
            }
            List<String> subDir2 = new ArrayList<>(logLines.subList(start, logLines.size()));

            return new LsOutput(parentDir, subDir1, subDir2);
        }

        private LsOutput(List<String> parentDir, List<String> subDir1, List<String> subDir2) {
            this.parentDir = parentDir;
            this.subDir1 = subDir1;
            this.subDir2 = subDir2;
        }
    }

    public static Consumer<String> endsWith(String suffix) {
        return value -> assertThat(value).endsWith(suffix);
    }
}

package org.testcontainers.utility;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

public class DirectoryTarResourceTest {

    @Test
    public void simpleRecursiveFileTest() throws TimeoutException {

        WaitingConsumer wait = new WaitingConsumer();

        final ToStringConsumer toString = new ToStringConsumer();

        // 'src' is expected to be the project base directory, so all source code/resources should be copied in
        File directory = new File("src");

        GenericContainer container = new GenericContainer(
                new ImageFromDockerfile()
                        .withDockerfileFromBuilder(builder ->
                                builder.from("alpine:3.3")
                                        .copy("/tmp/foo", "/foo")
                                        .cmd("cat /foo/test/resources/test-recursive-file.txt")
                                        .build()
                        ).withFileFromFile("/tmp/foo", directory))
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
                .withLogConsumer(wait.andThen(toString));

        container.start();
        wait.waitUntilEnd(60, TimeUnit.SECONDS);

        final String results = toString.toUtf8String();

        assertTrue("The container has a file that was copied in via a recursive copy", results.contains("Used for DirectoryTarResourceTest"));
    }

    @Test
    public void simpleRecursiveFileWithPermissionTest() throws TimeoutException {

        WaitingConsumer wait = new WaitingConsumer();

        final ToStringConsumer toString = new ToStringConsumer();

        GenericContainer container = new GenericContainer(
                new ImageFromDockerfile()
                        .withDockerfileFromBuilder(builder ->
                                builder.from("alpine:3.3")
                                        .copy("/tmp/foo", "/foo")
                                        .cmd("ls", "-al", "/")
                                        .build()
                        ).withFileFromFile("/tmp/foo", new File("/mappable-resource/test-resource.txt"),
                        0754))
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
                .withLogConsumer(wait.andThen(toString));

        container.start();
        wait.waitUntilEnd(60, TimeUnit.SECONDS);

        String listing = toString.toUtf8String();

        assertThat("Listing shows that file is copied with mode requested.",
                Arrays.asList(listing.split("\\n")),
                exactlyNItems(1, allOf(containsString("-rwxr-xr--"), containsString("foo"))));
    }

    @Test
    public void simpleRecursiveClasspathResourceTest() throws TimeoutException {
        // This test combines the copying of classpath resources from JAR files with the recursive TAR approach, to allow JARed classpath resources to be copied in to an image

        WaitingConsumer wait = new WaitingConsumer();

        final ToStringConsumer toString = new ToStringConsumer();

        GenericContainer container = new GenericContainer(
                new ImageFromDockerfile()
                        .withDockerfileFromBuilder(builder ->
                                builder.from("alpine:3.3")
                                        .copy("/tmp/foo", "/foo")
                                        .cmd("ls -lRt /foo")
                                        .build()
                        ).withFileFromClasspath("/tmp/foo", "/recursive/dir"))          // here we use /org/junit as a directory that really should exist on the classpath
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
                .withLogConsumer(wait.andThen(toString));

        container.start();
        wait.waitUntilEnd(60, TimeUnit.SECONDS);

        final String results = toString.toUtf8String();

        // ExternalResource.class is known to exist in a subdirectory of /org/junit so should be successfully copied in
        assertTrue("The container has a file that was copied in via a recursive copy from a JAR resource", results.contains("content.txt"));
    }

    public static <T> Matcher<Iterable<? super T>> exactlyNItems(final int n, Matcher<? super T> elementMatcher) {
        return new IsCollectionContaining<T>(elementMatcher) {
            @Override
            protected boolean matchesSafely(Iterable<? super T> collection, Description mismatchDescription) {
                int count = 0;
                boolean isPastFirst = false;

                for (Object item : collection) {

                    if (elementMatcher.matches(item)) {
                        count++;
                    }
                    if (isPastFirst) {
                        mismatchDescription.appendText(", ");
                    }
                    elementMatcher.describeMismatch(item, mismatchDescription);
                    isPastFirst = true;
                }

                if (count != n) {
                    mismatchDescription.appendText(". Expected exactly " + n + " but got " + count);
                }
                return count == n;
            }
        };
    }
}

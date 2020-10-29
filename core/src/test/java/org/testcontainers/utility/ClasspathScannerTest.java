package org.testcontainers.utility;

import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class ClasspathScannerTest {

    @Test
    public void realClassLoaderLookupOccurs() {
        // look for a resource that we know exists only once
        final List<URL> foundURLs = ClasspathScanner.scanFor("expectedClasspathFile.txt").collect(toList());

        assertEquals("Exactly one resource was found", 1, foundURLs.size());
    }

    @Test
    public void multipleResultsOnOneClassLoaderAreFound() throws IOException {
        final ClassLoader firstMockClassLoader = mock(ClassLoader.class);
        when(firstMockClassLoader.getResources(eq("someName"))).thenReturn(
            Collections.enumeration(
                asList(
                    new URL("file:///a/someName"),
                    new URL("file:///b/someName")
                )
            )
        );

        final List<URL> foundURLs = ClasspathScanner.scanFor("someName", firstMockClassLoader).collect(toList());
        assertEquals(
            "The expected URLs are found",
            asList(new URL("file:///a/someName"), new URL("file:///b/someName")),
            foundURLs
        );
    }

    @Test
    public void multipleClassLoadersAreQueried() throws IOException {
        final ClassLoader firstMockClassLoader = mock(ClassLoader.class);
        when(firstMockClassLoader.getResources(eq("someName"))).thenReturn(
            Collections.enumeration(
                asList(
                    new URL("file:///a/someName"),
                    new URL("file:///b/someName")
                )
            )
        );
        final ClassLoader secondMockClassLoader = mock(ClassLoader.class);
        when(secondMockClassLoader.getResources(eq("someName"))).thenReturn(
            Collections.enumeration(
                asList(
                    new URL("file:///b/someName"), // duplicate
                    new URL("file:///c/someName")
                )
            )
        );

        final List<URL> foundURLs = ClasspathScanner.scanFor("someName", firstMockClassLoader, secondMockClassLoader).collect(toList());

        assertEquals(
            "The expected URLs are found",
            asList(new URL("file:///a/someName"), new URL("file:///b/someName"), new URL("file:///c/someName")),
            foundURLs
        );
    }
}

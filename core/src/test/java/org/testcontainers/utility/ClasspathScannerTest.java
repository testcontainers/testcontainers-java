package org.testcontainers.utility;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ClasspathScannerTest {

    private static URL FILE_A;

    private static URL FILE_B;

    private static URL JAR_A;

    private static URL JAR_B;

    private static URL FILE_C;

    @BeforeClass
    public static void setUp() throws Exception {
        FILE_A = new URL("file:///a/someName");
        FILE_B = new URL("file:///b/someName");
        FILE_C = new URL("file:///c/someName");
        JAR_A = new URL("jar:file:a!/someName");
        JAR_B = new URL("jar:file:b!/someName");
    }

    @Test
    public void realClassLoaderLookupOccurs() {
        // look for a resource that we know exists only once
        final List<URL> foundURLs = ClasspathScanner.scanFor("expectedClasspathFile.txt").collect(Collectors.toList());

        assertThat(foundURLs).as("Exactly one resource was found").hasSize(1);
    }

    @Test
    public void multipleResultsOnOneClassLoaderAreFound() throws IOException {
        final ClassLoader firstMockClassLoader = mock(ClassLoader.class);
        when(firstMockClassLoader.getResources(eq("someName")))
            .thenReturn(Collections.enumeration(Arrays.asList(FILE_A, FILE_B)));

        final List<URL> foundURLs = ClasspathScanner
            .scanFor("someName", firstMockClassLoader)
            .collect(Collectors.toList());
        assertThat(foundURLs).as("The expected URLs are found").containsExactly(FILE_A, FILE_B);
    }

    @Test
    public void orderIsAlphabeticalForDeterminism() throws IOException {
        final ClassLoader firstMockClassLoader = mock(ClassLoader.class);
        when(firstMockClassLoader.getResources(eq("someName")))
            .thenReturn(Collections.enumeration(Arrays.asList(FILE_B, JAR_A, JAR_B, FILE_A)));

        final List<URL> foundURLs = ClasspathScanner
            .scanFor("someName", firstMockClassLoader)
            .collect(Collectors.toList());
        assertThat(foundURLs)
            .as("The expected URLs are found in the expected order")
            .containsExactly(FILE_A, FILE_B, JAR_A, JAR_B);
    }

    @Test
    public void multipleClassLoadersAreQueried() throws IOException {
        final ClassLoader firstMockClassLoader = mock(ClassLoader.class);
        when(firstMockClassLoader.getResources(eq("someName")))
            .thenReturn(Collections.enumeration(Arrays.asList(FILE_A, FILE_B)));
        final ClassLoader secondMockClassLoader = mock(ClassLoader.class);
        when(secondMockClassLoader.getResources(eq("someName")))
            .thenReturn(
                Collections.enumeration(
                    Arrays.asList(
                        FILE_B, // duplicate
                        FILE_C
                    )
                )
            );

        final List<URL> foundURLs = ClasspathScanner
            .scanFor("someName", firstMockClassLoader, secondMockClassLoader)
            .collect(Collectors.toList());

        assertThat(foundURLs).as("The expected URLs are found").containsExactly(FILE_A, FILE_B, FILE_C);
    }
}

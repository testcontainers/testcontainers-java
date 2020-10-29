package org.testcontainers.utility;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.net.URL;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Utility for identifying resource files on classloaders.
 */
@Slf4j
class ClasspathScanner {

    @VisibleForTesting
    static Stream<URL> scanFor(final String name, ClassLoader... classLoaders) {
        return Stream
            .of(classLoaders)
            .flatMap(classLoader -> getAllPropertyFilesOnClassloader(classLoader, name))
            .distinct()
            .filter(Objects::nonNull);
    }

    /**
     * @param name the resource name to search for
     * @return distinct, ordered stream of resources found by searching this class' classloader and then the current thread's context classloader
     */
    static Stream<URL> scanFor(final String name) {
        return scanFor(
            name,
            ClasspathScanner.class.getClassLoader(),
            Thread.currentThread().getContextClassLoader()
        );
    }

    @Nullable
    private static Stream<URL> getAllPropertyFilesOnClassloader(final ClassLoader it, final String s) {
        try {
            return Collections.list(it.getResources(s)).stream();
        } catch (Exception e) {
            log.error("Unable to read configuration from classloader {} - this is probably a bug", it, e);
            return Stream.empty();
        }
    }
}

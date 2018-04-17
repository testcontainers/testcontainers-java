package org.testcontainers;

import org.junit.Assume;
import org.junit.Test;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.containers.GenericContainer;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.instanceOf;

public class DaemonTest {

    public static void main(String[] args) {
        Thread mainThread = Thread.currentThread();

        GenericContainer genericContainer = null;

        try {
            genericContainer = new GenericContainer();
            genericContainer.start();

            Set<Thread> threads = new HashSet<>(Thread.getAllStackTraces().keySet());
            threads.remove(mainThread);

            Set<Thread> nonDaemonThreads = threads.stream().filter(it -> !it.isDaemon()).collect(Collectors.toSet());

            if (nonDaemonThreads.isEmpty()) {
                VisibleAssertions.pass("All threads marked as daemon");
            } else {
                String nonDaemonThreadNames = nonDaemonThreads.stream()
                    .map(Thread::getName)
                    .collect(Collectors.joining("\n", "\n", ""));

                VisibleAssertions.fail("Expected all threads to be daemons but the following are not:\n" + nonDaemonThreadNames);
            }
        } finally {
            if (genericContainer != null) {
                genericContainer.stop();
            }
        }
    }

    @Test
    public void testThatAllThreadsAreDaemons() throws Exception {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        Assume.assumeThat("context ClassLoader can return a list of classpath's URLs", contextClassLoader, instanceOf(URLClassLoader.class));

        String classpath = Stream.of(((URLClassLoader) contextClassLoader).getURLs())
            .map(URL::getFile)
            .collect(Collectors.joining(File.pathSeparator));

        ProcessBuilder processBuilder = new ProcessBuilder(
            System.getProperty("java.home") + "/bin/java",
            "-ea",
            "-classpath",
            classpath,
            DaemonTest.class.getCanonicalName()
        );

        assert processBuilder.inheritIO().start().waitFor() == 0;
    }
}

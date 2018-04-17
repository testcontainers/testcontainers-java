package org.testcontainers;

import org.junit.Test;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.containers.GenericContainer;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
        ProcessBuilder processBuilder = new ProcessBuilder(
            System.getProperty("java.home") + "/bin/java",
            "-ea",
            "-classpath",
            System.getProperty("java.class.path"),
            DaemonTest.class.getCanonicalName()
        );

        assert processBuilder.inheritIO().start().waitFor() == 0;
    }
}

package org.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * This test forks a new JVM, otherwise it's not possible to reliably diff the threads
 */
public class DaemonTest {

    public static void main(String[] args) {
        Thread mainThread = Thread.currentThread();

        GenericContainer<?> genericContainer = null;

        try {
            genericContainer = new GenericContainer<>(TestImages.TINY_IMAGE).withCommand("top");
            genericContainer.start();

            Set<Thread> threads = new HashSet<>(Thread.getAllStackTraces().keySet());
            threads.remove(mainThread);

            Set<Thread> nonDaemonThreads = threads.stream().filter(it -> !it.isDaemon()).collect(Collectors.toSet());

            if (!nonDaemonThreads.isEmpty()) {
                String nonDaemonThreadNames = nonDaemonThreads
                    .stream()
                    .map(Thread::getName)
                    .collect(Collectors.joining("\n", "\n", ""));

                fail("Expected all threads to be daemons but the following are not:\n" + nonDaemonThreadNames);
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
            new File(System.getProperty("java.home")).toPath().resolve("bin").resolve("java").toString(),
            "-ea",
            "-classpath",
            System.getProperty("java.class.path"),
            DaemonTest.class.getCanonicalName()
        );

        assertThat(processBuilder.inheritIO().start().waitFor()).isZero();
    }
}

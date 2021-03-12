package org.testcontainers.containers.output;

import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.TestImages.ALPINE_IMAGE;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDERR;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

public class ContainerLogsTest {

    @Test
    @Ignore("fails due to the timing of the shell's decision to flush")
    public void getLogsReturnsAllLogsToDate() {
        try (GenericContainer<?> container = shortLivedContainer()) {
            container.start();

            final String logs = container.getLogs();
            assertEquals("stdout and stderr are reflected in the returned logs", "stdout\nstderr", logs);
        }
    }

    @Test
    public void getLogsContainsBothOutputTypes() {
        try (GenericContainer<?> container = shortLivedContainer()) {
            container.start();

            // docsGetAllLogs {
            final String logs = container.getLogs();
            // }
            assertTrue("stdout is reflected in the returned logs", logs.contains("stdout"));
            assertTrue("stderr is reflected in the returned logs", logs.contains("stderr"));
        }
    }

    @Test
    public void getLogsReturnsStdOutToDate() {
        try (GenericContainer<?> container = shortLivedContainer()) {
            container.start();

            // docsGetStdOut {
            final String logs = container.getLogs(STDOUT);
            // }
            assertTrue("stdout is reflected in the returned logs", logs.contains("stdout"));
        }
    }

    @Test
    public void getLogsReturnsStdErrToDate() {
        try (GenericContainer<?> container = shortLivedContainer()) {
            container.start();

            // docsGetStdErr {
            final String logs = container.getLogs(STDERR);
            // }
            assertTrue("stderr is reflected in the returned logs", logs.contains("stderr"));
        }
    }

    @Test
    public void getLogsForLongRunningContainer() throws InterruptedException {
        try (GenericContainer<?> container = longRunningContainer()) {
            container.start();

            Thread.sleep(1000L);

            final String logs = container.getLogs(STDOUT);
            assertTrue("stdout is reflected in the returned logs for a running container", logs.contains("seq=0"));
        }
    }

    private static GenericContainer<?> shortLivedContainer() {
        return new GenericContainer<>(ALPINE_IMAGE)
            .withCommand("/bin/sh", "-c", "echo -n 'stdout' && echo -n 'stderr' 1>&2")
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy());
    }

    private static GenericContainer<?> longRunningContainer() {
        return new GenericContainer<>(ALPINE_IMAGE)
            .withCommand("ping -c 100 127.0.0.1");
    }
}

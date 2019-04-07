package org.testcontainers.containers.output;

import java.util.Arrays;
import org.hamcrest.collection.IsIn;
import org.hamcrest.core.AnyOf;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import static org.hamcrest.Matchers.isIn;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDERR;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

public class ContainerLogsTest {

    @Test
    public void getLogsReturnsAllLogsToDate() {
        try (GenericContainer container = shortLivedContainer()) {
            container.start();

            // docsGetAllLogs {
            final String logs = container.getLogs();
            assertThat("stdout and stderr from container logs", logs, isIn(new String[]{"stdout\nstderr", "stderr\nstdout"}));
        }
    }

    @Test
    public void getLogsReturnsStdOutToDate() {
        try (GenericContainer container = shortLivedContainer()) {
            container.start();

            // docsGetStdOut {
            final String logs = container.getLogs(STDOUT);
            // }
            assertEquals("stdout and stderr are reflected in the returned logs", "stdout", logs);
        }
    }

    @Test
    public void getLogsReturnsStdErrToDate() {
        try (GenericContainer container = shortLivedContainer()) {
            container.start();

            // docsGetStdErr {
            final String logs = container.getLogs(STDERR);
            // }
            assertEquals("stdout and stderr are reflected in the returned logs", "stderr", logs);
        }
    }

    @Test
    public void getLogsForLongRunningContainer() throws InterruptedException {
        try (GenericContainer container = longRunningContainer()) {
            container.start();

            Thread.sleep(1000L);

            final String logs = container.getLogs(STDOUT);
            assertTrue("stdout is reflected in the returned logs for a running container", logs.contains("seq=0"));
        }
    }

    private static GenericContainer shortLivedContainer() {
        return new GenericContainer("alpine:3.3")
            .withCommand("/bin/sh", "-c", "echo -n 'stdout' && echo -n 'stderr' 1>&2")
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy());
    }

    private static GenericContainer longRunningContainer() {
        return new GenericContainer("alpine:3.3")
            .withCommand("ping -c 100 127.0.0.1");
    }
}

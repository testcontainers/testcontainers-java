package org.testcontainers.containers.output;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDERR;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

public class ContainerLogsTest {

    @Rule
    public GenericContainer container = new GenericContainer("alpine:3.3")
        .withCommand("/bin/sh", "-c", "echo -n 'stdout' && echo -n 'stderr' 1>&2")
        .withStartupCheckStrategy(new OneShotStartupCheckStrategy());

    @Test
    public void getLogsReturnsAllLogsToDate() {
        final String logs = container.getLogs();
        assertEquals("stdout and stderr are reflected in the returned logs", "stdout\nstderr", logs);
    }

    @Test
    public void getLogsReturnsStdOutToDate() {
        final String logs = container.getLogs(STDOUT);
        assertEquals("stdout and stderr are reflected in the returned logs", "stdout", logs);
    }

    @Test
    public void getLogsReturnsStdErrToDate() {
        final String logs = container.getLogs(STDERR);
        assertEquals("stdout and stderr are reflected in the returned logs", "stderr", logs);
    }
}

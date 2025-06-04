package org.testcontainers.containers.output;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import static org.assertj.core.api.Assertions.assertThat;

public class ContainerLogsTest {

    @Test
    @Disabled("fails due to the timing of the shell's decision to flush")
    public void getLogsReturnsAllLogsToDate() {
        try (GenericContainer<?> container = shortLivedContainer()) {
            container.start();

            final String logs = container.getLogs();
            assertThat(logs).as("stdout and stderr are reflected in the returned logs").isEqualTo("stdout\nstderr");
        }
    }

    @Test
    public void getLogsContainsBothOutputTypes() {
        try (GenericContainer<?> container = shortLivedContainer()) {
            container.start();

            // docsGetAllLogs {
            final String logs = container.getLogs();
            // }
            assertThat(logs).as("stdout is reflected in the returned logs").contains("stdout");
            assertThat(logs).as("stderr is reflected in the returned logs").contains("stderr");
        }
    }

    @Test
    public void getLogsReturnsStdOutToDate() {
        try (GenericContainer<?> container = shortLivedContainer()) {
            container.start();

            // docsGetStdOut {
            final String logs = container.getLogs(OutputFrame.OutputType.STDOUT);
            // }
            assertThat(logs).as("stdout is reflected in the returned logs").contains("stdout");
        }
    }

    @Test
    public void getLogsReturnsStdErrToDate() {
        try (GenericContainer<?> container = shortLivedContainer()) {
            container.start();

            // docsGetStdErr {
            final String logs = container.getLogs(OutputFrame.OutputType.STDERR);
            // }
            assertThat(logs).as("stderr is reflected in the returned logs").contains("stderr");
        }
    }

    @Test
    public void getLogsForLongRunningContainer() throws InterruptedException {
        try (GenericContainer<?> container = longRunningContainer()) {
            container.start();

            Thread.sleep(1000L);

            final String logs = container.getLogs(OutputFrame.OutputType.STDOUT);
            assertThat(logs).as("stdout is reflected in the returned logs for a running container").contains("seq=0");
        }
    }

    private static GenericContainer<?> shortLivedContainer() {
        return new GenericContainer<>(TestImages.ALPINE_IMAGE)
            .withCommand("/bin/sh", "-c", "echo -n 'stdout' && echo -n 'stderr' 1>&2")
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy());
    }

    private static GenericContainer<?> longRunningContainer() {
        return new GenericContainer<>(TestImages.ALPINE_IMAGE).withCommand("ping -c 100 127.0.0.1");
    }
}

package org.testcontainers.containers.output;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

public class ToStringConsumerTest {

    private static final String LARGE_PAYLOAD;

    static {
        StringBuilder builder = new StringBuilder(10_003 * 10);;
        for (int i = 0; i < 10; i++) {
            builder.append(' ').append(i).append(RandomStringUtils.randomAlphabetic(10000));
        }
        LARGE_PAYLOAD = builder.toString();
        Assertions.assertThat(LARGE_PAYLOAD).doesNotContain("\n");
    }

    @Test
    public void newlines_are_not_added_to_exec_output() throws Exception {
        try (GenericContainer<?> container = new GenericContainer<>("alpine:3.13")) {
            container.withCommand("sleep", "2m");
            container.start();

            ExecResult build = container.execInContainer("echo", "-n", LARGE_PAYLOAD);
            Assertions.assertThat(build.getStdout())
                .doesNotContain("\n")
                .isEqualTo(LARGE_PAYLOAD);
        }
    }

    @Test(timeout = 60_000L)
    public void newlines_are_not_added_to_exec_output_with_tty() throws Exception {
        try (GenericContainer<?> container = new GenericContainer<>("alpine:3.13")) {
            container.withCreateContainerCmdModifier(cmd -> {
                cmd
                    .withAttachStdin(true)
                    .withStdinOpen(true)
                    .withTty(true);
            });
            container.withCommand("sleep", "2m");
            container.start();

            ExecResult build = container.execInContainer("echo", "-n", LARGE_PAYLOAD);
            assertThat(build.getStdout())
                .isEqualTo(LARGE_PAYLOAD)
                .doesNotContain("\n");
        }
    }

    @Test
    public void newlines_are_not_added_to_container_output() {
        try (GenericContainer<?> container = new GenericContainer<>("alpine:3.13")) {
            container.withCommand("echo", "-n", LARGE_PAYLOAD);
            container.setStartupCheckStrategy(new OneShotStartupCheckStrategy());
            container.start();

            container.getDockerClient().waitContainerCmd(container.getContainerId()).start().awaitStatusCode();

            assertThat(container.getLogs())
                .isEqualTo(LARGE_PAYLOAD)
                .doesNotContain("\n");
        }
    }

    @Test
    public void newlines_are_not_added_to_container_output_with_tty() {
        try (GenericContainer<?> container = new GenericContainer<>("alpine:3.13")) {
            container.withCreateContainerCmdModifier(cmd -> {
                cmd.withTty(true);
            });
            container.withCommand("echo", "-n", LARGE_PAYLOAD);
            container.setStartupCheckStrategy(new OneShotStartupCheckStrategy());
            container.start();

            container.getDockerClient().waitContainerCmd(container.getContainerId()).start().awaitStatusCode();

            assertThat(container.getLogs())
                .isEqualTo(LARGE_PAYLOAD)
                .doesNotContain("\n");
        }
    }
}

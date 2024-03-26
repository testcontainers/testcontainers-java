package org.testcontainers.containers.output;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.File;
import java.io.FileWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ToFileConsumerTest {

    private static final String LARGE_PAYLOAD;

    static {
        StringBuilder builder = new StringBuilder(10_003 * 10);
        for (int i = 0; i < 10; i++) {
            builder.append(' ').append(i).append(RandomStringUtils.randomAlphabetic(10000));
        }
        LARGE_PAYLOAD = builder.toString();
        assertThat(LARGE_PAYLOAD).doesNotContain("\n");
    }

    private final File logFile = new File("test.log");

    @Test
    public void large_output_stored_in_new_file() throws Exception {
        try (GenericContainer<?> container = new GenericContainer<>("alpine:3")) {
            // given
            logFile.delete();
            assertThat(logFile).doesNotExist();

            container.withLogConsumer(new ToFileConsumer(logFile));
            container.withCommand("echo", "-n", LARGE_PAYLOAD);

            // when
            container.start();

            // then
            assertThat(container.getLogs()).doesNotContain("\n").isEqualTo(LARGE_PAYLOAD);
            assertThat(logFile).exists().content().doesNotContain("\n").isEqualTo(LARGE_PAYLOAD);
        }
    }

    @Test
    public void error_stored_in_new_file() throws Exception {
        try (GenericContainer<?> container = new GenericContainer<>("alpine:3")) {
            // given
            logFile.delete();
            assertThat(logFile).doesNotExist();

            container.withLogConsumer(new ToFileConsumer(logFile));
            container.withCommand(
                "awk",
                "BEGIN {print \"error from awk\" > \"/dev/stderr\"; print \"output from awk\" > \"/dev/stdout\"}"
            );

            // when
            container.start();

            // then
            assertThat(container.getLogs()).contains("error from awk").contains("output from awk");
            assertThat(logFile).exists().content().contains("error from awk\n").contains("output from awk\n");
        }
    }

    @Test
    public void output_stored_in_new_file() throws Exception {
        try (GenericContainer<?> container = new GenericContainer<>("alpine:3")) {
            // given
            logFile.delete();
            assertThat(logFile).doesNotExist();

            container.withLogConsumer(new ToFileConsumer(logFile));
            container.withCommand("echo", "something");

            // when
            container.start();

            // then
            assertThat(container.getLogs()).contains("something");
            assertThat(logFile).exists().hasContent("something\n");
        }
    }

    @Test
    public void output_stored_in_existing_file() throws Exception {
        try (
            GenericContainer<?> container = new GenericContainer<>("alpine:3");
            FileWriter writer = new FileWriter(logFile)
        ) {
            // given
            assertThat(logFile).exists().isEmpty();

            writer.write("existing content\n");
            writer.close();

            boolean writable = logFile.setWritable(false);
            assertThat(writable).isTrue();

            assertThat(logFile).exists().hasContent("existing content\n");

            container.withLogConsumer(new ToFileConsumer(logFile));
            container.withCommand("echo", "something");

            // when
            container.start();

            // then
            assertThat(container.getLogs()).contains("something");
            assertThat(logFile).exists().hasContent("existing content\n");
        }
    }

    @Test
    public void output_failed_to_be_stored_in_existing_file() throws Exception {
        try (
            GenericContainer<?> container = new GenericContainer<>("alpine:3");
            FileWriter writer = new FileWriter(logFile)
        ) {
            // given
            assertThat(logFile).exists().isEmpty();

            writer.write("existing content\n");
            writer.close();

            assertThat(logFile).exists().hasContent("existing content\n");

            container.withLogConsumer(new ToFileConsumer(logFile));
            container.withCommand("echo", "something");

            // when
            container.start();

            // then
            assertThat(container.getLogs()).contains("something");
            assertThat(logFile).exists().hasContent("existing content\nsomething\n");
        }
    }

    @Test
    public void cannot_create_consumer_with_null_file() throws Exception {
        assertThatThrownBy(() -> new ToFileConsumer(null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("logFile must not be null");
    }

    @After
    public void tearDown() {
        logFile.delete();
    }
}

package org.testcontainers.k6;

import org.junit.Test;
import org.testcontainers.containers.output.WaitingConsumer;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class K6ContainerTests {

    private final String INJECT_SCRIPT_VAR = "are cool!";

    @Test
    public void k6StandardTest() throws Exception {
        try (
            // standard_k6 {
            K6Container container =
                new K6Container()
                    .withTestScript("scripts/test.js")
                    .withScriptVar("MY_SCRIPT_VAR", INJECT_SCRIPT_VAR)
                    .withCmdOptions("--quiet", "--no-usage-report")
            // }
        ) {
            container.start();

            WaitingConsumer consumer = new WaitingConsumer();
            container.followOutput(consumer);

            // Wait for test script results to be collected
            consumer.waitUntil(frame ->
                frame.getUtf8String().contains("iteration_duration"), 3, TimeUnit.SECONDS);

            assertThat(container.getLogs()).contains("k6 tests " + INJECT_SCRIPT_VAR);
        }
    }

    @Test
    public void k6ExtendedTest() throws Exception {
        try (
            // extended_k6 {
            K6Container container =
                    new K6Container(K6Container.K6_BUILDER_IMAGE)
                            .withTestScript("scripts/extensions.js")
                            .withCmdOptions("--quiet", "--no-usage-report")
            // }
        ) {
            container.start();

            WaitingConsumer consumer = new WaitingConsumer();
            container.followOutput(consumer);

            // Wait for test script results to be collected
            consumer.waitUntil(frame ->
                    frame.getUtf8String().contains("iteration_duration"), 5, TimeUnit.MINUTES);

            assertThat(container.getLogs()).contains("k6 tests extended");
        }
    }

}

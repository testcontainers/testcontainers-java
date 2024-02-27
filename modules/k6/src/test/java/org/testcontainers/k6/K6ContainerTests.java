package org.testcontainers.k6;

import org.junit.Test;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class K6ContainerTests {

    @Test
    public void k6StandardTest() throws Exception {
        try (
            // standard_k6 {
            K6Container container =
                new K6Container("grafana/k6:0.49.0")
                    .withTestScript(
                        MountableFile.forClasspathResource("scripts/test.js"))
                    .withScriptVar("MY_SCRIPT_VAR", "are cool!")
                    .withScriptVar("AN_UNUSED_VAR", "unused")
                    .withCmdOptions("--quiet", "--no-usage-report")
            // }
        ) {
            container.start();

            WaitingConsumer consumer = new WaitingConsumer();
            container.followOutput(consumer);

            // Wait for test script results to be collected
            consumer.waitUntil(frame -> {
                return frame.getUtf8String().contains("iteration_duration");
            }, 3, TimeUnit.SECONDS);

            assertThat(container.getLogs()).contains("k6 tests are cool!");
        }
    }

}

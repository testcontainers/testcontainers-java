package org.testcontainers.containers.wait.strategy;

import org.junit.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class HostPortWaitStrategyTest {

    @Test
    public void toStringIncludesStartupTimeout() {
        WaitStrategy strategy = new HostPortWaitStrategy()
            .withStartupTimeout(Duration.ofSeconds(60));

        String output = strategy.toString();

        assertThat(output)
            .contains("HostPortWaitStrategy")
            .contains("startupTimeout=PT1M");
    }
}

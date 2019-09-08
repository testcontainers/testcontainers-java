package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GenericContainerTest {

    @Test
    public void shouldReportOOMAfterWait() {
        try (
            GenericContainer container = new GenericContainer<>()
                .waitingFor(new WaitForState(ContainerState::getOOMKilled))
                .withCreateContainerCmdModifier(it -> {
                    it.getHostConfig().withMemory(4 * FileUtils.ONE_MB);
                })
                .withCommand("sh", "-c", "usleep 100; dd if=/dev/urandom bs=32MB count=1 > test.txt")
        ) {
            assertThatThrownBy(container::start)
                .hasStackTraceContaining("Container crashed with out-of-memory");
        }
    }

    @Test
    public void shouldReportErrorAfterWait() {
        try (
            GenericContainer container = new GenericContainer<>()
                .waitingFor(new WaitForState(state -> state.getExitCode() > 0))
                .withCommand("sh", "-c", "usleep 100; exit 123")
        ) {
            assertThatThrownBy(container::start)
                .hasStackTraceContaining("Container exited with code 123");
        }
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true)
    static class WaitForState extends AbstractWaitStrategy {

        Predicate<ContainerState> predicate;

        @Override
        @SneakyThrows
        protected void waitUntilReady() {
            Unreliables.retryUntilTrue(5, TimeUnit.SECONDS, () -> {
                ContainerState state = waitStrategyTarget.getCurrentContainerInfo().getState();
                return predicate.test(state);
            });

            throw new IllegalStateException("Nope!");
        }
    }
}

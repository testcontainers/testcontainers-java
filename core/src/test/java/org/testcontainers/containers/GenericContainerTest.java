package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.model.Info;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assumptions;
import org.junit.Test;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.TestImages;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GenericContainerTest {

    @Test
    public void shouldReportOOMAfterWait() {
        Info info = DockerClientFactory.instance().client().infoCmd().exec();
        // Poor man's rootless Docker detection :D
        Assumptions.assumeThat(info.getSecurityOptions()).doesNotContain("rootless");
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withStartupCheckStrategy(new NoopStartupCheckStrategy())
                .waitingFor(new WaitForExitedState(ContainerState::getOOMKilled))
                .withCreateContainerCmdModifier(it -> {
                    it.getHostConfig()
                        .withMemory(20 * FileUtils.ONE_MB)
                        .withMemorySwappiness(0L)
                        .withMemorySwap(0L)
                        .withMemoryReservation(0L)
                        .withKernelMemory(16 * FileUtils.ONE_MB);
                })
                .withCommand("sh", "-c", "A='0123456789'; for i in $(seq 0 32); do A=$A$A; done; sleep 10m")
        ) {
            assertThatThrownBy(container::start)
                .hasStackTraceContaining("Container crashed with out-of-memory");
        }
    }

    @Test
    public void shouldReportErrorAfterWait() {
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withStartupCheckStrategy(new NoopStartupCheckStrategy())
                .waitingFor(new WaitForExitedState(state -> state.getExitCode() > 0))
                .withCommand("sh", "-c", "usleep 100; exit 123")
        ) {
            assertThatThrownBy(container::start)
                .hasStackTraceContaining("Container exited with code 123");
        }
    }

    static class NoopStartupCheckStrategy extends StartupCheckStrategy {

        @Override
        public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
            return StartupStatus.SUCCESSFUL;
        }
    }

    @RequiredArgsConstructor
    @FieldDefaults(makeFinal = true)
    @Slf4j
    static class WaitForExitedState extends AbstractWaitStrategy {

        Predicate<ContainerState> predicate;

        @Override
        @SneakyThrows
        protected void waitUntilReady() {
            Unreliables.retryUntilTrue(5, TimeUnit.SECONDS, () -> {
                ContainerState state = waitStrategyTarget.getCurrentContainerInfo().getState();

                log.debug("Current state: {}", state);
                if (!"exited".equalsIgnoreCase(state.getStatus())) {
                    Thread.sleep(100);
                    return false;
                }
                return predicate.test(state);
            });

            throw new IllegalStateException("Nope!");
        }
    }
}

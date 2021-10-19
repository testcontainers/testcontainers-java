package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Ports;
import com.google.common.primitives.Ints;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assumptions;
import org.junit.Test;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.TestImages;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.TestImages.TINY_IMAGE;

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

    @Test
    public void shouldOnlyPublishExposedPorts() {
        ImageFromDockerfile image = new ImageFromDockerfile("publish-multiple")
            .withDockerfileFromBuilder(builder ->
                builder
                    .from("testcontainers/helloworld:1.1.0")
                    .expose(8080, 8081)
                    .build()
            );
        try (GenericContainer<?> container = new GenericContainer<>(image).withExposedPorts(8080)) {
            container.start();

            InspectContainerResponse inspectedContainer = container.getContainerInfo();

            List<Integer> exposedPorts = Arrays.stream(inspectedContainer.getConfig().getExposedPorts())
                .map(ExposedPort::getPort)
                .collect(Collectors.toList());

            assertEquals(
                "the exposed ports are all of those EXPOSEd by the image",
                Ints.asList(8080, 8081),
                exposedPorts
            );

            Map<ExposedPort, Ports.Binding[]> hostBindings = inspectedContainer.getHostConfig().getPortBindings().getBindings();
            assertEquals(
                "only 1 port is bound on the host (published)",
                1,
                hostBindings.size()
            );

            Integer mappedPort = container.getMappedPort(8080);
            assertTrue(
                "port 8080 is bound to a different port on the host",
                mappedPort != 8080
            );

            assertThrows(
                "trying to get a non-bound port mapping fails",
                IllegalArgumentException.class,
                () -> {
                    container.getMappedPort(8081);
                }
            );
        }
    }

    @Test
    public void shouldWaitUntilExposedPortIsMapped() {

        ImageFromDockerfile image = new ImageFromDockerfile("publish-multiple")
            .withDockerfileFromBuilder(builder ->
                builder
                    .from("testcontainers/helloworld:1.1.0")
                    .expose(8080, 8081) // one additional port exposed in image
                    .build()
            );

        try (
            GenericContainer container = new GenericContainer<>(image)
                .withExposedPorts(8080)
                .withCreateContainerCmdModifier(it -> it.withExposedPorts(ExposedPort.tcp(8082))) // another port exposed by modifier
        ) {

            container.start();

            assertEquals("Only withExposedPort should be exposed", 1, container.getExposedPorts().size());
            assertTrue("withExposedPort should be exposed", container.getExposedPorts().contains(8080));
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

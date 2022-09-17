package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.Ports;
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
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.MountableFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.SystemUtils.IS_OS_LINUX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

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
                    it
                        .getHostConfig()
                        .withMemory(20 * FileUtils.ONE_MB)
                        .withMemorySwappiness(0L)
                        .withMemorySwap(0L)
                        .withMemoryReservation(0L)
                        .withKernelMemory(16 * FileUtils.ONE_MB);
                })
                .withCommand("sh", "-c", "A='0123456789'; for i in $(seq 0 32); do A=$A$A; done; sleep 10m")
        ) {
            assertThatThrownBy(container::start).hasStackTraceContaining("Container crashed with out-of-memory");
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
            assertThatThrownBy(container::start).hasStackTraceContaining("Container exited with code 123");
        }
    }

    @Test
    public void shouldCopyTransferableAsFile() {
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withStartupCheckStrategy(new NoopStartupCheckStrategy())
                .withCopyToContainer(Transferable.of("test"), "/tmp/test")
                .waitingFor(new WaitForExitedState(state -> state.getExitCodeLong() > 0))
                .withCommand("sh", "-c", "grep -q test /tmp/test && exit 100")
        ) {
            assertThatThrownBy(container::start).hasStackTraceContaining("Container exited with code 100");
        }
    }

    @Test
    public void shouldCopyTransferableAsFileWithFileMode() {
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withStartupCheckStrategy(new NoopStartupCheckStrategy())
                .withCopyToContainer(Transferable.of("test", 0777), "/tmp/test")
                .waitingFor(new WaitForExitedState(state -> state.getExitCodeLong() > 0))
                .withCommand("sh", "-c", "ls -ll /tmp | grep '\\-rwxrwxrwx\\|test' && exit 100")
        ) {
            assertThatThrownBy(container::start).hasStackTraceContaining("Container exited with code 100");
        }
    }

    @Test
    public void shouldCopyTransferableAfterMountableFile() {
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withStartupCheckStrategy(new NoopStartupCheckStrategy())
                .withCopyFileToContainer(MountableFile.forClasspathResource("test_copy_to_container.txt"), "/tmp/test")
                .withCopyToContainer(Transferable.of("test"), "/tmp/test")
                .waitingFor(new WaitForExitedState(state -> state.getExitCodeLong() > 0))
                .withCommand("sh", "-c", "grep -q test /tmp/test && exit 100")
        ) {
            assertThatThrownBy(container::start).hasStackTraceContaining("Container exited with code 100");
        }
    }

    @Test
    public void shouldOnlyPublishExposedPorts() {
        ImageFromDockerfile image = new ImageFromDockerfile("publish-multiple")
            .withDockerfileFromBuilder(builder -> {
                builder
                    .from("testcontainers/helloworld:1.1.0") //
                    .expose(8080, 8081)
                    .build();
            });
        try (GenericContainer<?> container = new GenericContainer<>(image).withExposedPorts(8080)) {
            container.start();

            InspectContainerResponse inspectedContainer = container.getContainerInfo();

            List<Integer> exposedPorts = Arrays
                .stream(inspectedContainer.getConfig().getExposedPorts())
                .map(ExposedPort::getPort)
                .collect(Collectors.toList());

            assertThat(exposedPorts).as("the exposed ports are all of those EXPOSEd by the image").contains(8080, 8081);

            Map<ExposedPort, Ports.Binding[]> hostBindings = inspectedContainer
                .getHostConfig()
                .getPortBindings()
                .getBindings();
            assertThat(hostBindings).as("only 1 port is bound on the host (published)").hasSize(1);

            Integer mappedPort = container.getMappedPort(8080);
            assertThat(mappedPort != 8080).as("port 8080 is bound to a different port on the host").isTrue();

            assertThat(catchThrowable(() -> container.getMappedPort(8081)))
                .as("trying to get a non-bound port mapping fails")
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void shouldWaitUntilExposedPortIsMapped() {
        ImageFromDockerfile image = new ImageFromDockerfile("publish-multiple")
            .withDockerfileFromBuilder(builder -> {
                builder
                    .from("testcontainers/helloworld:1.1.0")
                    .expose(8080, 8081) // one additional port exposed in image
                    .build();
            });

        try (
            GenericContainer container = new GenericContainer<>(image)
                .withExposedPorts(8080)
                .withCreateContainerCmdModifier(it -> it.withExposedPorts(ExposedPort.tcp(8082))) // another port exposed by modifier
        ) {
            container.start();

            assertThat(container.getExposedPorts()).as("Only withExposedPort should be exposed").hasSize(1);
            assertThat(container.getExposedPorts()).as("withExposedPort should be exposed").contains(8080);
        }
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionIfGetMappedPortIsCalledOnHostNetworkMode() {
        if (!IS_OS_LINUX) {
            // Host networking mode is only supported in Linux, thus skip test on other platforms
            return;
        }
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.REDIS_IMAGE)
                .withNetworkMode("host")
                .withExposedPorts(6379)
        ) {
            container.start();
            assertThatThrownBy(() -> container.getMappedPort(6379)).isInstanceOf(IllegalArgumentException.class);
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
            Unreliables.retryUntilTrue(
                5,
                TimeUnit.SECONDS,
                () -> {
                    ContainerState state = waitStrategyTarget.getCurrentContainerInfo().getState();

                    log.debug("Current state: {}", state);
                    if (!"exited".equalsIgnoreCase(state.getStatus())) {
                        Thread.sleep(100);
                        return false;
                    }
                    return predicate.test(state);
                }
            );

            throw new IllegalStateException("Nope!");
        }
    }
}

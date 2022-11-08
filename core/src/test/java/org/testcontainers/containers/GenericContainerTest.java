package org.testcontainers.containers;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assumptions.assumeThat;

public class GenericContainerTest {

    @Test
    public void shouldReportOOMAfterWait() {
        Info info = DockerClientFactory.instance().client().infoCmd().exec();
        // Poor man's rootless Docker detection :D
        Assumptions.assumeThat(info.getSecurityOptions()).doesNotContain("rootless");
        // setting swappiness is not allowed for cgroups v2
        Assumptions.assumeThat(info.getRawValues().get("CgroupVersion")).isNotEqualTo("2");
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
            assertThatThrownBy(container::start)
                .hasStackTraceContaining("Wait strategy failed. Container crashed with out-of-memory (OOMKilled)")
                .hasStackTraceContaining("Nope!");
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
                .hasStackTraceContaining("Container startup failed for image " + TestImages.TINY_IMAGE)
                .hasStackTraceContaining("Wait strategy failed. Container exited with code 123")
                .hasStackTraceContaining("Nope!");
        }
    }

    @Test
    public void shouldCopyTransferableAsFile() {
        try (
            // transferableFile {
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withStartupCheckStrategy(new NoopStartupCheckStrategy())
                .withCopyToContainer(Transferable.of("test"), "/tmp/test")
                .waitingFor(new WaitForExitedState(state -> state.getExitCodeLong() > 0))
                .withCommand("sh", "-c", "grep -q test /tmp/test && exit 100")
            // }
        ) {
            assertThatThrownBy(container::start)
                .hasStackTraceContaining("Wait strategy failed. Container exited with code 100")
                .hasStackTraceContaining("Nope!");
        }
    }

    @Test
    public void shouldCopyTransferableAsFileWithFileMode() {
        try (
            // transferableWithFileMode {
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withStartupCheckStrategy(new NoopStartupCheckStrategy())
                .withCopyToContainer(Transferable.of("test", 0777), "/tmp/test")
                .waitingFor(new WaitForExitedState(state -> state.getExitCodeLong() > 0))
                .withCommand("sh", "-c", "ls -ll /tmp | grep '\\-rwxrwxrwx\\|test' && exit 100")
            // }
        ) {
            assertThatThrownBy(container::start)
                .hasStackTraceContaining("Wait strategy failed. Container exited with code 100")
                .hasStackTraceContaining("Nope!");
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
            assertThatThrownBy(container::start)
                .hasStackTraceContaining("Wait strategy failed. Container exited with code 100")
                .hasStackTraceContaining("Nope!");
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
            // podman also returns unbound ports, but sets the binding value to null
            List<Ports.Binding[]> boundPorts = hostBindings
                .values()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            assertThat(boundPorts).as("only 1 port is bound on the host (published)").hasSize(1);

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
    public void testArchitectureCheck() {
        assumeThat(DockerClientFactory.instance().client().versionCmd().exec().getArch()).isNotEqualTo("amd64");
        // Choose an image that is *different* from the server architecture--this ensures we always get a warning.
        final String image;
        if (DockerClientFactory.instance().client().versionCmd().exec().getArch().equals("amd64")) {
            // arm64 image
            image = "testcontainers/sshd@sha256:f701fa4ae2cd25ad2b2ea2df1aad00980f67bacdd03958a2d7d52ee63d7fb3e8";
        } else {
            // amd64 image
            image = "testcontainers/sshd@sha256:7879c6c99eeab01f1c6beb2c240d49a70430ef2d52f454765ec9707f547ef6f1";
        }

        try (GenericContainer container = new GenericContainer<>(image)) {
            // Grab a copy of everything that is logged when we start the container
            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) container.logger();
            ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
            listAppender.start();
            logger.addAppender(listAppender);

            container.start();

            String regexMatch = "The architecture '\\S+' for image .*";
            assertThat(listAppender.list)
                .describedAs(
                    "Received log list does not have a message matching '" +
                    regexMatch +
                    "': " +
                    listAppender.list.toString()
                )
                .filteredOn(event -> event.getMessage().matches(regexMatch))
                .isNotEmpty();
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

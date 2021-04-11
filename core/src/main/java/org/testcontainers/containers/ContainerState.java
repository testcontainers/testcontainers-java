package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.HealthState;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.google.common.base.Preconditions;
import lombok.SneakyThrows;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.LogUtils;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.ThrowingFunction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public interface ContainerState {

    String STATE_HEALTHY = "healthy";

    /**
     * Get the IP address that this container may be reached on (may not be the local machine).
     *
     * @return an IP address
     * @see #getHost()
     */
    default String getContainerIpAddress() {
        return getHost();
    }

    /**
     * Get the host that this container may be reached on (may not be the local machine).
     *
     * @return a host
     */
    default String getHost() {
        return DockerClientFactory.instance().dockerHostIpAddress();
    }

    /**
     * @return is the container currently running?
     */
    default boolean isRunning() {
        if (getContainerId() == null) {
            return false;
        }

        try {
            Boolean running = getCurrentContainerInfo().getState().getRunning();
            return Boolean.TRUE.equals(running);
        } catch (DockerException e) {
            return false;
        }
    }

    /**
     * @return is the container created?
     */
    default boolean isCreated() {
        if (getContainerId() == null) {
            return false;
        }

        try {
            String status = getCurrentContainerInfo().getState().getStatus();
            return "created".equalsIgnoreCase(status) || isRunning();
        } catch (DockerException e) {
            return false;
        }
    }

    /**
     * @return has the container health state 'healthy'?
     */
    default boolean isHealthy() {
        if (getContainerId() == null) {
            return false;
        }

        try {
            InspectContainerResponse inspectContainerResponse = getCurrentContainerInfo();
            HealthState health = inspectContainerResponse.getState().getHealth();
            if (health == null) {
                throw new RuntimeException("This container's image does not have a healthcheck declared, so health cannot be determined. Either amend the image or use another approach to determine whether containers are healthy.");
            }

            return STATE_HEALTHY.equals(health.getStatus());
        } catch (DockerException e) {
            return false;
        }
    }

    default InspectContainerResponse getCurrentContainerInfo() {
        return DockerClientFactory.instance().client().inspectContainerCmd(getContainerId()).exec();
    }

    /**
     * Get the actual mapped port for a first port exposed by the container.
     * Should be used in conjunction with {@link #getHost()}.
     *
     * @return the port that the exposed port is mapped to
     * @throws IllegalStateException if there are no exposed ports
     */
    default Integer getFirstMappedPort() {
        return getExposedPorts()
            .stream()
            .findFirst()
            .map(this::getMappedPort)
            .orElseThrow(() -> new IllegalStateException("Container doesn't expose any ports"));
    }

    /**
     * Get the actual mapped port for a given port exposed by the container.
     * Should be used in conjunction with {@link #getHost()}.
     *
     * @param originalPort the original TCP port that is exposed
     * @return the port that the exposed port is mapped to, or null if it is not exposed
     */
    default Integer getMappedPort(int originalPort) {
        Preconditions.checkState(this.getContainerId() != null, "Mapped port can only be obtained after the container is started");

        Ports.Binding[] binding = new Ports.Binding[0];
        final InspectContainerResponse containerInfo = this.getContainerInfo();
        if (containerInfo != null) {
            binding = containerInfo.getNetworkSettings().getPorts().getBindings().get(new ExposedPort(originalPort));
        }

        if (binding != null && binding.length > 0 && binding[0] != null) {
            return Integer.valueOf(binding[0].getHostPortSpec());
        } else {
            throw new IllegalArgumentException("Requested port (" + originalPort + ") is not mapped");
        }
    }

    /**
     * @return the exposed ports
     */
    List<Integer> getExposedPorts();

    /**
     * @return the port bindings
     */
    default List<String> getPortBindings() {
        List<String> portBindings = new ArrayList<>();
        final Ports hostPortBindings = this.getContainerInfo().getHostConfig().getPortBindings();
        for (Map.Entry<ExposedPort, Ports.Binding[]> binding : hostPortBindings.getBindings().entrySet()) {
            for (Ports.Binding portBinding : binding.getValue()) {
                portBindings.add(String.format("%s:%s", portBinding.toString(), binding.getKey()));
            }
        }
        return portBindings;
    }

    /**
     * @return the bound port numbers
     */
    default List<Integer> getBoundPortNumbers() {
        return getPortBindings().stream()
            .map(PortBinding::parse)
            .map(PortBinding::getBinding)
            .map(Ports.Binding::getHostPortSpec)
            .filter(Objects::nonNull)
            .filter(NumberUtils::isNumber)
            .map(Integer::valueOf)
            .filter(port -> port > 0)
            .collect(Collectors.toList());
    }


    /**
     * @return all log output from the container from start until the current instant (both stdout and stderr)
     */
    default String getLogs() {
        return LogUtils.getOutput(DockerClientFactory.instance().client(), getContainerId());
    }

    /**
     * @param types log types to return
     * @return all log output from the container from start until the current instant
     */
    default String getLogs(OutputFrame.OutputType... types) {
        return LogUtils.getOutput(DockerClientFactory.instance().client(), getContainerId(), types);
    }

    /**
     * @return the id of the container
     */
    default String getContainerId() {
        return getContainerInfo().getId();
    }

    /**
     * @return the container info
     */
    InspectContainerResponse getContainerInfo();

    /**
     * Run a command inside a running container, as though using "docker exec", and interpreting
     * the output as UTF8.
     * <p>
     * @see ExecInContainerPattern#execInContainer(com.github.dockerjava.api.command.InspectContainerResponse, String...)
     */
    default Container.ExecResult execInContainer(String... command) throws UnsupportedOperationException, IOException, InterruptedException {
        return execInContainer(StandardCharsets.UTF_8, command);
    }

    /**
     * Run a command inside a running container, as though using "docker exec".
     * <p>
     * @see ExecInContainerPattern#execInContainer(com.github.dockerjava.api.command.InspectContainerResponse, Charset, String...)
     */
    default Container.ExecResult execInContainer(Charset outputCharset, String... command) throws UnsupportedOperationException, IOException, InterruptedException {
        return ExecInContainerPattern.execInContainer(getContainerInfo(), outputCharset, command);
    }

    /**
     *
     * Copies a file or directory to the container.
     *
     * @param mountableFile file or directory which is copied into the container
     * @param containerPath destination path inside the container
     */
    default void copyFileToContainer(MountableFile mountableFile, String containerPath) {
        File sourceFile = new File(mountableFile.getResolvedPath());

        if (containerPath.endsWith("/") && sourceFile.isFile()) {
            final Logger logger = LoggerFactory.getLogger(GenericContainer.class);
            logger.warn("folder-like containerPath in copyFileToContainer is deprecated, please explicitly specify a file path");
            copyFileToContainer((Transferable) mountableFile, containerPath + sourceFile.getName());
        } else {
            copyFileToContainer((Transferable) mountableFile, containerPath);
        }
    }

    /**
     *
     * Copies a file to the container.
     *
     * @param transferable file which is copied into the container
     * @param containerPath destination path inside the container
     */
    @SneakyThrows(IOException.class)
    default void copyFileToContainer(Transferable transferable, String containerPath) {
        if (getContainerId() == null) {
            throw new IllegalStateException("copyFileToContainer can only be used with created / running container");
        }

        try (
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            TarArchiveOutputStream tarArchive = new TarArchiveOutputStream(byteArrayOutputStream)
        ) {
            tarArchive.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            transferable.transferTo(tarArchive, containerPath);
            tarArchive.finish();

            DockerClientFactory.instance().client()
                .copyArchiveToContainerCmd(getContainerId())
                .withTarInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()))
                .withRemotePath("/")
                .exec();
        }
    }

    /**
     * Copies a file which resides inside the container to user defined directory
     *
     * @param containerPath path to file which is copied from container
     * @param destinationPath destination path to which file is copied with file name
     * @throws IOException if there's an issue communicating with Docker or receiving entry from TarArchiveInputStream
     * @throws InterruptedException if the thread waiting for the response is interrupted
     */
    default void copyFileFromContainer(String containerPath, String destinationPath) throws IOException, InterruptedException {
        copyFileFromContainer(containerPath, inputStream -> {
            try(FileOutputStream output = new FileOutputStream(destinationPath)) {
                IOUtils.copy(inputStream, output);
                return null;
            }
        });
    }

    /**
     * Streams a file which resides inside the container
     *
     * @param containerPath path to file which is copied from container
     * @param function function that takes InputStream of the copied file
     */
    @SneakyThrows
    default  <T> T copyFileFromContainer(String containerPath, ThrowingFunction<InputStream, T> function) {
        if (getContainerId() == null) {
            throw new IllegalStateException("copyFileFromContainer can only be used when the Container is created.");
        }

        DockerClient dockerClient = DockerClientFactory.instance().client();
        try (
            InputStream inputStream = dockerClient.copyArchiveFromContainerCmd(getContainerId(), containerPath).exec();
            TarArchiveInputStream tarInputStream = new TarArchiveInputStream(inputStream)
        ) {
            tarInputStream.getNextTarEntry();
            return function.apply(tarInputStream);
        }
    }
}

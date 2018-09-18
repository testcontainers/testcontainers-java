package org.testcontainers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.io.*;
import java.net.Socket;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Cleans allocated docker resources via side container.
 *
 * See <a href="https://github.com/testcontainers/moby-ryuk">https://github.com/testcontainers/moby-ryuk</a>.
 */
@Slf4j
final class RyakResourceManager extends ResourceManagerBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(RyakResourceManager.class);
    private static final List<List<Map.Entry<String, String>>> DEATH_NOTE = new ArrayList<>();
    private final String hostIpAddress;

    RyakResourceManager(DockerClient dockerClient, String hostIpAddress) {
        super(dockerClient);
        this.hostIpAddress = hostIpAddress;
    }

    @Override
    public void initialize() {
        boolean checksEnabled = !TestcontainersConfiguration.getInstance().isDisableChecks();
        String ryukContainerId = start(hostIpAddress, dockerClient, checksEnabled);
        LOGGER.info("Ryuk started - will monitor and terminate Testcontainers containers on JVM exit");

        if (checksEnabled) {
            checkDiskSpace(ryukContainerId);
            checkMountableFile(ryukContainerId);
        }
    }

    @SneakyThrows(InterruptedException.class)
    private String start(String hostIpAddress, DockerClient client, boolean withDummyMount) {
        String ryukImage = TestcontainersConfiguration.getInstance().getRyukImage();
        DockerClientFactory.instance().checkAndPullImage(client, ryukImage);

        MountableFile mountableFile = MountableFile.forClasspathResource(RyakResourceManager.class.getName().replace(".", "/") + ".class");

        List<Bind> binds = new ArrayList<>();
        binds.add(new Bind("//var/run/docker.sock", new Volume("/var/run/docker.sock")));
        if (withDummyMount) {
            // Not needed for Ryuk, but we perform pre-flight checks with it (micro optimization)
            binds.add(new Bind(mountableFile.getResolvedPath(), new Volume("/dummy"), AccessMode.ro));
        }

        String ryukContainerId = client.createContainerCmd(ryukImage)
                .withHostConfig(new HostConfig() {
                    @JsonProperty("AutoRemove")
                    boolean autoRemove = true;
                })
                .withExposedPorts(new ExposedPort(8080))
                .withPublishAllPorts(true)
                .withName("testcontainers-ryuk-" + DockerClientFactory.SESSION_ID)
                .withLabels(Collections.singletonMap(DockerClientFactory.TESTCONTAINERS_LABEL, "true"))
                .withBinds(binds)
                .exec()
                .getId();

        client.startContainerCmd(ryukContainerId).exec();

        InspectContainerResponse inspectedContainer = client.inspectContainerCmd(ryukContainerId).exec();

        Integer ryukPort = inspectedContainer.getNetworkSettings().getPorts().getBindings().values().stream()
                .flatMap(Stream::of)
                .findFirst()
                .map(Ports.Binding::getHostPortSpec)
                .map(Integer::parseInt)
                .get();

        CountDownLatch ryukScheduledLatch = new CountDownLatch(1);

        synchronized (DEATH_NOTE) {
            DEATH_NOTE.add(
                    DockerClientFactory.DEFAULT_LABELS.entrySet().stream()
                            .<Map.Entry<String, String>>map(it -> new SimpleEntry<>("label", it.getKey() + "=" + it.getValue()))
                            .collect(Collectors.toList())
            );
        }

        Thread kiraThread = new Thread(
                DockerClientFactory.TESTCONTAINERS_THREAD_GROUP,
                () -> {
                    while (true) {
                        int index = 0;
                        try(Socket clientSocket = new Socket(hostIpAddress, ryukPort)) {
                            FilterRegistry registry = new FilterRegistry(clientSocket.getInputStream(), clientSocket.getOutputStream());

                            synchronized (DEATH_NOTE) {
                                while (true) {
                                    if (DEATH_NOTE.size() <= index) {
                                        try {
                                            DEATH_NOTE.wait(1_000);
                                            continue;
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                    List<Map.Entry<String, String>> filters = DEATH_NOTE.get(index);
                                    boolean isAcknowledged = registry.register(filters);
                                    if (isAcknowledged) {
                                        log.debug("Received 'ACK' from Ryuk");
                                        ryukScheduledLatch.countDown();
                                        index++;
                                    } else {
                                        log.debug("Didn't receive 'ACK' from Ryuk. Will retry to send filters.");
                                    }
                                }
                            }
                        } catch (IOException e) {
                            log.warn("Can not connect to Ryuk at {}:{}", hostIpAddress, ryukPort, e);
                        }
                    }
                },
                "testcontainers-ryuk"
        );
        kiraThread.setDaemon(true);
        kiraThread.start();

        // We need to wait before we can start any containers to make sure that we delete them
        if (!ryukScheduledLatch.await(TestcontainersConfiguration.getInstance().getRyukTimeout(), TimeUnit.SECONDS)) {
            throw new IllegalStateException("Can not connect to Ryuk");
        }

        return ryukContainerId;
    }

    /**
     * Register a filter to be cleaned up.
     *
     * @param filter the filter
     */
    public void registerFilterForCleanup(List<Map.Entry<String, String>> filter) {
        synchronized (DEATH_NOTE) {
            DEATH_NOTE.add(filter);
            DEATH_NOTE.notifyAll();
        }
    }

    private void checkDiskSpace(String id) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            dockerClient
                .execStartCmd(dockerClient.execCreateCmd(id).withAttachStdout(true).withCmd("df", "-P").exec().getId())
                .exec(new ExecStartResultCallback(outputStream, null))
                .awaitCompletion();
        } catch (Exception e) {
            log.debug("Can't exec disk checking command", e);
        }

        DiskSpaceUsage df = DiskSpaceUsage.parseAvailableDiskSpace(outputStream.toString());

        VisibleAssertions.assertTrue(
            "Docker environment should have more than 2GB free disk space",
            df.getAvailableMB().map(it -> it >= 2048).orElse(true)
        );
    }

    private void checkMountableFile(String id) {
        try (InputStream stream = dockerClient.copyArchiveFromContainerCmd(id, "/dummy").exec()) {
            stream.read();
            VisibleAssertions.pass("File should be mountable");
        } catch (Exception e) {
            VisibleAssertions.fail("File should be mountable but fails with " + e.getMessage());
        }
    }
}

package org.testcontainers.utility;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.DockerException;
import com.github.dockerjava.api.exception.InternalServerErrorException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.AccessMode;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Component that responsible for container removal and automatic cleanup of dead containers at JVM shutdown.
 */
@Slf4j
public final class ResourceReaper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceReaper.class);

    private static final List<List<Map.Entry<String, String>>> DEATH_NOTE = new ArrayList<>();

    private static ResourceReaper instance;
    private final DockerClient dockerClient;
    private Map<String, String> registeredContainers = new ConcurrentHashMap<>();
    private Set<String> registeredNetworks = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private AtomicBoolean hookIsSet = new AtomicBoolean(false);

    private ResourceReaper() {
        dockerClient = DockerClientFactory.instance().client();
    }

    public static String start(String hostIpAddress, DockerClient client) {
        return start(hostIpAddress, client, false);
    }

    @SneakyThrows(InterruptedException.class)
    public static String start(String hostIpAddress, DockerClient client, boolean withDummyMount) {
        String ryukImage = TestcontainersConfiguration.getInstance().getRyukImage();
        DockerClientFactory.instance().checkAndPullImage(client, ryukImage);

        MountableFile mountableFile = MountableFile.forClasspathResource(ResourceReaper.class.getName().replace(".", "/") + ".class");

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
                            OutputStream out = clientSocket.getOutputStream();
                            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

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

                                    String query = URLEncodedUtils.format(
                                            filters.stream()
                                                    .map(it -> new BasicNameValuePair(it.getKey(), it.getValue()))
                                                    .collect(Collectors.toList()),
                                            (String) null
                                    );

                                    log.debug("Sending '{}' to Ryuk", query);
                                    out.write(query.getBytes());
                                    out.write('\n');
                                    out.flush();

                                    while (!"ACK".equalsIgnoreCase(in.readLine())) {
                                    }

                                    ryukScheduledLatch.countDown();
                                    index++;
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

    public synchronized static ResourceReaper instance() {
        if (instance == null) {
            instance = new ResourceReaper();
        }

        return instance;
    }

    /**
     * Perform a cleanup.
     *
     */
    public synchronized void performCleanup() {
        registeredContainers.forEach(this::stopContainer);
        registeredNetworks.forEach(this::removeNetwork);
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

    /**
     * Register a container to be cleaned up, either on explicit call to stopAndRemoveContainer, or at JVM shutdown.
     *
     * @param containerId the ID of the container
     * @param imageName   the image name of the container (used for logging)
     */
    public void registerContainerForCleanup(String containerId, String imageName) {
        setHook();
        registeredContainers.put(containerId, imageName);
    }

    /**
     * Stop a potentially running container and remove it, including associated volumes.
     *
     * @param containerId the ID of the container
     */
    public void stopAndRemoveContainer(String containerId) {
        stopContainer(containerId, registeredContainers.get(containerId));

        registeredContainers.remove(containerId);
    }

    /**
     * Stop a potentially running container and remove it, including associated volumes.
     *
     * @param containerId the ID of the container
     * @param imageName   the image name of the container (used for logging)
     */
    public void stopAndRemoveContainer(String containerId, String imageName) {
        stopContainer(containerId, imageName);

        registeredContainers.remove(containerId);
    }

    private void stopContainer(String containerId, String imageName) {
        boolean running;
        try {
            InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerId).exec();
            running = containerInfo.getState().getRunning();
        } catch (NotFoundException e) {
            LOGGER.trace("Was going to stop container but it apparently no longer exists: {}");
            return;
        } catch (DockerException e) {
            LOGGER.trace("Error encountered when checking container for shutdown (ID: {}) - it may not have been stopped, or may already be stopped: {}", containerId, e.getMessage());
            return;
        }

        if (running) {
            try {
                LOGGER.trace("Stopping container: {}", containerId);
                dockerClient.killContainerCmd(containerId).exec();
                LOGGER.trace("Stopped container: {}", imageName);
            } catch (DockerException e) {
                LOGGER.trace("Error encountered shutting down container (ID: {}) - it may not have been stopped, or may already be stopped: {}", containerId, e.getMessage());
            }
        }

        try {
            dockerClient.inspectContainerCmd(containerId).exec();
        } catch (NotFoundException e) {
            LOGGER.trace("Was going to remove container but it apparently no longer exists: {}");
            return;
        }

        try {
            LOGGER.trace("Removing container: {}", containerId);
            try {
                dockerClient.removeContainerCmd(containerId).withRemoveVolumes(true).withForce(true).exec();
                LOGGER.debug("Removed container and associated volume(s): {}", imageName);
            } catch (InternalServerErrorException e) {
                LOGGER.trace("Exception when removing container with associated volume(s): {} (due to {})", imageName, e.getMessage());
            }
        } catch (DockerException e) {
            LOGGER.trace("Error encountered shutting down container (ID: {}) - it may not have been stopped, or may already be stopped: {}", containerId, e.getMessage());
        }
    }

    /**
     * Register a network to be cleaned up at JVM shutdown.
     *
     * @param id   the ID of the network
     */
    public void registerNetworkIdForCleanup(String id) {
        setHook();
        registeredNetworks.add(id);
    }

    /**
     * @param networkName   the name of the network
     * @deprecated see {@link ResourceReaper#registerNetworkIdForCleanup(String)}
     */
    @Deprecated
    public void registerNetworkForCleanup(String networkName) {
        try {
            // Try to find the network by name, so that we can register its ID for later deletion
            dockerClient.listNetworksCmd()
                    .withNameFilter(networkName)
                    .exec()
            .forEach(network -> registerNetworkIdForCleanup(network.getId()));
        } catch (Exception e) {
            LOGGER.trace("Error encountered when looking up network (name: {})", networkName);
        }
    }

    /**
     * Removes a network by ID.
     * @param id
     */
    public void removeNetworkById(String id) {
      removeNetwork(id);
    }

    /**
     * Removes a network by ID.
     * @param identifier
     * @deprecated see {@link ResourceReaper#removeNetworkById(String)}
     */
    @Deprecated
    public void removeNetworks(String identifier) {
        removeNetworkById(identifier);
    }

    private void removeNetwork(String id) {
        try {
            List<Network> networks;
            try {
                // Try to find the network if it still exists
                // Listing by ID first prevents docker-java logging an error if we just go blindly into removeNetworkCmd
                networks = dockerClient.listNetworksCmd().withIdFilter(id).exec();
            } catch (Exception e) {
                LOGGER.trace("Error encountered when looking up network for removal (name: {}) - it may not have been removed", id);
                return;
            }

            // at this point networks should contain either 0 or 1 entries, depending on whether the network exists
            // using a for loop we essentially treat the network like an optional, only applying the removal if it exists
            for (Network network : networks) {
                try {
                    dockerClient.removeNetworkCmd(network.getId()).exec();
                    registeredNetworks.remove(network.getId());
                    LOGGER.debug("Removed network: {}", id);
                } catch (Exception e) {
                    LOGGER.trace("Error encountered removing network (name: {}) - it may not have been removed", network.getName());
                }
            }
        } finally {
            registeredNetworks.remove(id);
        }
    }

    public void unregisterNetwork(String identifier) {
        registeredNetworks.remove(identifier);
    }

    public void unregisterContainer(String identifier) {
        registeredContainers.remove(identifier);
    }

    private void setHook() {
        if (hookIsSet.compareAndSet(false, true)) {
            // If the JVM stops without containers being stopped, try and stop the container.
            Runtime.getRuntime().addShutdownHook(new Thread(DockerClientFactory.TESTCONTAINERS_THREAD_GROUP, this::performCleanup));
        }
    }
}

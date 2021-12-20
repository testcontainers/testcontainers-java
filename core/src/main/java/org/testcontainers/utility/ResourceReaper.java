package org.testcontainers.utility;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;

/**
 * Component that responsible for container removal and automatic cleanup of dead containers at JVM shutdown.
 */
@Slf4j
public final class ResourceReaper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceReaper.class);

    private static final List<List<Map.Entry<String, String>>> DEATH_NOTE = new ArrayList<>();
    private static final RateLimiter RYUK_ACK_RATE_LIMITER = RateLimiterBuilder
        .newBuilder()
        .withRate(4, TimeUnit.SECONDS)
        .withConstantThroughput()
        .build();

    private static ResourceReaper instance;
    private final DockerClient dockerClient;
    private Map<String, String> registeredContainers = new ConcurrentHashMap<>();
    private Set<String> registeredNetworks = Sets.newConcurrentHashSet();
    private Set<String> registeredImages = Sets.newConcurrentHashSet();
    private AtomicBoolean hookIsSet = new AtomicBoolean(false);

    private ResourceReaper() {
        dockerClient = DockerClientFactory.instance().client();
    }


    /**
     *
     * @deprecated internal API
     */
    @Deprecated
    public static String start(String hostIpAddress, DockerClient client) {
        return start(client);
    }

    /**
     *
     * @deprecated internal API
     */
    @Deprecated
    @SneakyThrows(InterruptedException.class)
    public static String start(DockerClient client) {
        String ryukImage = ImageNameSubstitutor.instance()
            .apply(DockerImageName.parse("testcontainers/ryuk:0.3.3"))
            .asCanonicalNameString();
        DockerClientFactory.instance().checkAndPullImage(client, ryukImage);

        List<Bind> binds = new ArrayList<>();
        binds.add(new Bind(DockerClientFactory.instance().getRemoteDockerUnixSocketPath(), new Volume("/var/run/docker.sock")));

        ExposedPort ryukExposedPort = ExposedPort.tcp(8080);
        String ryukContainerId = client.createContainerCmd(ryukImage)
                .withHostConfig(
                    new HostConfig()
                        .withAutoRemove(true)
                        .withPortBindings(new PortBinding(Ports.Binding.empty(), ryukExposedPort))
                )
                .withExposedPorts(ryukExposedPort)
                .withName("testcontainers-ryuk-" + DockerClientFactory.SESSION_ID)
                .withLabels(Collections.singletonMap(DockerClientFactory.TESTCONTAINERS_LABEL, "true"))
                .withBinds(binds)
                .withPrivileged(TestcontainersConfiguration.getInstance().isRyukPrivileged())
                .exec()
                .getId();

        client.startContainerCmd(ryukContainerId).exec();

        StringBuilder ryukLog = new StringBuilder();

        ResultCallback.Adapter<Frame> logCallback = client.logContainerCmd(ryukContainerId)
            .withSince(0)
            .withFollowStream(true)
            .withStdOut(true)
            .withStdErr(true)
            .exec(new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame frame) {
                    ryukLog.append(new String(frame.getPayload(), StandardCharsets.UTF_8));
                }
            });

        ContainerState containerState = new ContainerState() {

            // inspect container response might initially not contain the mapped port
            final InspectContainerResponse inspectedContainer = await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(DynamicPollInterval.ofMillis(50))
                .pollInSameThread()
                .until(
                    () -> client.inspectContainerCmd(ryukContainerId).exec(),
                    inspectContainerResponse -> {
                        return inspectContainerResponse
                        .getNetworkSettings()
                        .getPorts()
                        .getBindings()
                        .values()
                        .stream()
                        .anyMatch(Objects::nonNull);
                    }
                );

            @Override
            public List<Integer> getExposedPorts() {
                return Stream.of(getContainerInfo().getConfig().getExposedPorts())
                    .map(ExposedPort::getPort)
                    .collect(Collectors.toList());
            }

            @Override
            public InspectContainerResponse getContainerInfo() {
                return inspectedContainer;
            }
        };

        CountDownLatch ryukScheduledLatch = new CountDownLatch(1);

        synchronized (DEATH_NOTE) {
            DEATH_NOTE.add(
                    DockerClientFactory.DEFAULT_LABELS.entrySet().stream()
                            .<Map.Entry<String, String>>map(it -> new SimpleEntry<>("label", it.getKey() + "=" + it.getValue()))
                            .collect(Collectors.toList())
            );
        }

        String host = containerState.getHost();
        Integer ryukPort = containerState.getFirstMappedPort();
        Thread kiraThread = new Thread(
                DockerClientFactory.TESTCONTAINERS_THREAD_GROUP,
                () -> {
                    while (true) {
                        RYUK_ACK_RATE_LIMITER.doWhenReady(() -> {
                            int index = 0;
                            // not set the read timeout, as Ryuk would not send anything unless a new filter is submitted, meaning that we would get a timeout exception pretty quick
                            try (Socket clientSocket = new Socket()) {
                                clientSocket.connect(new InetSocketAddress(host, ryukPort), 5 * 1000);
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
                                log.warn("Can not connect to Ryuk at {}:{}", host, ryukPort, e);
                            }
                        });
                    }
                },
                "testcontainers-ryuk"
        );
        kiraThread.setDaemon(true);
        kiraThread.start();
        try {
            // We need to wait before we can start any containers to make sure that we delete them
            if (!ryukScheduledLatch.await(TestcontainersConfiguration.getInstance().getRyukTimeout(), TimeUnit.SECONDS)) {
                log.error("Timed out waiting for Ryuk container to start. Ryuk's logs:\n{}", ryukLog);
                throw new IllegalStateException(String.format("Could not connect to Ryuk at %s:%s", host, ryukPort));
            }
        } finally {
            try {
                logCallback.close();
            } catch (IOException ignored) {
            }
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
     */
    public synchronized void performCleanup() {
        registeredContainers.forEach(this::stopContainer);
        registeredNetworks.forEach(this::removeNetwork);
        registeredImages.forEach(this::removeImage);
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
            running = containerInfo.getState() != null && Boolean.TRUE.equals(containerInfo.getState().getRunning());
        } catch (NotFoundException e) {
            LOGGER.trace("Was going to stop container but it apparently no longer exists: {}", containerId);
            return;
        } catch (Exception e) {
            LOGGER.trace("Error encountered when checking container for shutdown (ID: {}) - it may not have been stopped, or may already be stopped. Root cause: {}",
                containerId,
                Throwables.getRootCause(e).getMessage());
            return;
        }

        if (running) {
            try {
                LOGGER.trace("Stopping container: {}", containerId);
                dockerClient.killContainerCmd(containerId).exec();
                LOGGER.trace("Stopped container: {}", imageName);
            } catch (Exception e) {
                LOGGER.trace("Error encountered shutting down container (ID: {}) - it may not have been stopped, or may already be stopped. Root cause: {}",
                    containerId,
                    Throwables.getRootCause(e).getMessage());
            }
        }

        try {
            dockerClient.inspectContainerCmd(containerId).exec();
        } catch (Exception e) {
            LOGGER.trace("Was going to remove container but it apparently no longer exists: {}", containerId);
            return;
        }

        try {
            LOGGER.trace("Removing container: {}", containerId);
            dockerClient.removeContainerCmd(containerId).withRemoveVolumes(true).withForce(true).exec();
            LOGGER.debug("Removed container and associated volume(s): {}", imageName);
        } catch (Exception e) {
            LOGGER.trace("Error encountered shutting down container (ID: {}) - it may not have been stopped, or may already be stopped. Root cause: {}",
                containerId,
                Throwables.getRootCause(e).getMessage());
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

    public void registerImageForCleanup(String dockerImageName) {
        setHook();
        registeredImages.add(dockerImageName);
    }

    private void removeImage(String dockerImageName) {
        LOGGER.trace("Removing image tagged {}", dockerImageName);
        try {
            dockerClient.removeImageCmd(dockerImageName).withForce(true).exec();
        } catch (Throwable e) {
            LOGGER.warn("Unable to delete image " + dockerImageName, e);
        }
    }

    private void setHook() {
        if (hookIsSet.compareAndSet(false, true)) {
            // If the JVM stops without containers being stopped, try and stop the container.
            Runtime.getRuntime().addShutdownHook(new Thread(DockerClientFactory.TESTCONTAINERS_THREAD_GROUP, this::performCleanup));
        }
    }

    static class FilterRegistry {

        @VisibleForTesting
        static final String ACKNOWLEDGMENT = "ACK";

        private final BufferedReader in;
        private final OutputStream out;

        FilterRegistry(InputStream ryukInputStream, OutputStream ryukOutputStream) {
            this.in = new BufferedReader(new InputStreamReader(ryukInputStream));
            this.out = ryukOutputStream;
        }

        /**
         * Registers the given filters with Ryuk
         *
         * @param filters the filter to register
         * @return true if the filters have been registered successfuly, false otherwise
         * @throws IOException if communication with Ryuk fails
         */
        protected boolean register(List<Map.Entry<String, String>> filters) throws IOException {
            String query = filters.stream()
                .map(it -> {
                    try {
                        return URLEncoder.encode(it.getKey(), "UTF-8") + "=" + URLEncoder.encode(it.getValue(), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.joining("&"));

            log.debug("Sending '{}' to Ryuk", query);
            out.write(query.getBytes());
            out.write('\n');
            out.flush();

            return waitForAcknowledgment(in);
        }

        private static boolean waitForAcknowledgment(BufferedReader in) throws IOException {
            String line = in.readLine();
            while (line != null && !ACKNOWLEDGMENT.equalsIgnoreCase(line)) {
                line = in.readLine();
            }
            return ACKNOWLEDGMENT.equalsIgnoreCase(line);
        }

    }
}

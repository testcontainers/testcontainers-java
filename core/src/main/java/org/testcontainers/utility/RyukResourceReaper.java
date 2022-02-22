package org.testcontainers.utility;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Volume;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;

/**
 * Ryuk-based {@link ResourceReaper} implementation.
 *
 * @see <a href="https://github.com/testcontainers/moby-ryuk">moby-ryuk</a>
 * @deprecated internal API
 */
@Deprecated
@Slf4j
public class RyukResourceReaper extends ResourceReaper {

    private static final RateLimiter RYUK_ACK_RATE_LIMITER = RateLimiterBuilder
        .newBuilder()
        .withRate(4, TimeUnit.SECONDS)
        .withConstantThroughput()
        .build();

    private final AtomicBoolean started = new AtomicBoolean(false);

    @Getter
    private String containerId = null;

    @Override
    public void init() {
        if (!TestcontainersConfiguration.getInstance().environmentSupportsReuse()) {
            log.debug("Ryuk is enabled");
            maybeStart();
            log.info("Ryuk started - will monitor and terminate Testcontainers containers on JVM exit");
        } else {
            log.debug("Ryuk is enabled but will be started on demand");
        }
    }

    @Override
    public void registerLabelsFilterForCleanup(Map<String, String> labels) {
        maybeStart();
        super.registerLabelsFilterForCleanup(labels);
    }

    @Override
    public Map<String, String> getLabels() {
        maybeStart();
        return super.getLabels();
    }

    @SneakyThrows(InterruptedException.class)
    private synchronized void maybeStart() {
        if (!started.compareAndSet(false, true)) {
            return;
        }
        DockerClient client = DockerClientFactory.lazyClient();
        String ryukImage = ImageNameSubstitutor.instance()
            .apply(DockerImageName.parse("testcontainers/ryuk:0.3.3"))
            .asCanonicalNameString();
        DockerClientFactory.instance().checkAndPullImage(client, ryukImage);

        List<Bind> binds = new ArrayList<>();
        binds.add(new Bind(DockerClientFactory.instance().getRemoteDockerUnixSocketPath(), new Volume("/var/run/docker.sock")));

        ExposedPort ryukExposedPort = ExposedPort.tcp(8080);
        containerId = client.createContainerCmd(ryukImage)
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

        client.startContainerCmd(containerId).exec();

        StringBuilder ryukLog = new StringBuilder();

        ResultCallback.Adapter<Frame> logCallback = client.logContainerCmd(containerId)
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

        InspectContainerResponse inspectedContainer;
        try {
            // inspect container response might initially not contain the mapped port
            inspectedContainer = await()
                .atMost(5, TimeUnit.SECONDS)
                .pollInterval(DynamicPollInterval.ofMillis(50))
                .pollInSameThread()
                .until(
                    () -> client.inspectContainerCmd(containerId).exec(),
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
        } catch (Exception e) {
            log.warn("Ryuk container cannot be inspected and probably had a problem starting. Ryuk's logs:\n{}", ryukLog);
            throw new IllegalStateException("Ryuk failed to start", e);
        }

        ContainerState containerState = new ContainerState() {

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
                            ResourceReaper.FilterRegistry registry = new ResourceReaper.FilterRegistry(clientSocket.getInputStream(), clientSocket.getOutputStream());

                            synchronized (ResourceReaper.DEATH_NOTE) {
                                while (true) {
                                    if (ResourceReaper.DEATH_NOTE.size() <= index) {
                                        try {
                                            ResourceReaper.DEATH_NOTE.wait(1_000);
                                            continue;
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                    List<Map.Entry<String, String>> filters = ResourceReaper.DEATH_NOTE.get(index);
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
    }
}

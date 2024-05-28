package org.testcontainers.utility;

import com.github.dockerjava.api.command.CreateContainerCmd;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ryuk-based {@link ResourceReaper} implementation.
 *
 * @see <a href="https://github.com/testcontainers/moby-ryuk">moby-ryuk</a>
 */
@Slf4j
class RyukResourceReaper extends ResourceReaper {

    private static final RateLimiter RYUK_ACK_RATE_LIMITER = RateLimiterBuilder
        .newBuilder()
        .withRate(4, TimeUnit.SECONDS)
        .withConstantThroughput()
        .build();

    private final AtomicBoolean started = new AtomicBoolean(false);

    private final RyukContainer ryukContainer = new RyukContainer();

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

    @Override
    public CreateContainerCmd register(GenericContainer<?> container, CreateContainerCmd cmd) {
        if (container == ryukContainer) {
            // Do not register Ryuk container to avoid self-pruning
            return cmd;
        }

        maybeStart();
        return super.register(container, cmd);
    }

    @SneakyThrows(InterruptedException.class)
    private synchronized void maybeStart() {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        ryukContainer.start();

        if (TestcontainersConfiguration.getInstance().isRyukShutdownHookEnabled()) {
            Runtime
                .getRuntime()
                .addShutdownHook(
                    new Thread(
                        DockerClientFactory.TESTCONTAINERS_THREAD_GROUP,
                        () -> {
                            this.dockerClient.killContainerCmd(this.ryukContainer.getContainerId())
                                .withSignal("SIGTERM")
                                .exec();
                        }
                    )
                );
        }

        CountDownLatch ryukScheduledLatch = new CountDownLatch(1);

        String host = ryukContainer.getHost();
        Integer ryukPort = ryukContainer.getFirstMappedPort();
        Thread kiraThread = new Thread(
            DockerClientFactory.TESTCONTAINERS_THREAD_GROUP,
            () -> {
                while (true) {
                    RYUK_ACK_RATE_LIMITER.doWhenReady(() -> {
                        int index = 0;
                        // not set the read timeout, as Ryuk would not send anything unless a new filter is submitted, meaning that we would get a timeout exception pretty quick
                        try (Socket clientSocket = new Socket()) {
                            clientSocket.connect(new InetSocketAddress(host, ryukPort), 5 * 1000);
                            ResourceReaper.FilterRegistry registry = new ResourceReaper.FilterRegistry(
                                clientSocket.getInputStream(),
                                clientSocket.getOutputStream()
                            );

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
        // We need to wait before we can start any containers to make sure that we delete them
        if (!ryukScheduledLatch.await(TestcontainersConfiguration.getInstance().getRyukTimeout(), TimeUnit.SECONDS)) {
            log.error("Timed out waiting for Ryuk container to start. Ryuk's logs:\n{}", ryukContainer.getLogs());
            throw new IllegalStateException(String.format("Could not connect to Ryuk at %s:%s", host, ryukPort));
        }
    }
}

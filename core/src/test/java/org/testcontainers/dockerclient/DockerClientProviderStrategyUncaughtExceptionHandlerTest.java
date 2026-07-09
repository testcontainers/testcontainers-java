package org.testcontainers.dockerclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testcontainers.utility.MockTestcontainersConfigurationExtension;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.net.ServerSocket;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockTestcontainersConfigurationExtension.class)
class DockerClientProviderStrategyUncaughtExceptionHandlerTest {

    @Test
    void doesNotReplaceGlobalUncaughtExceptionHandler() throws Exception {
        // Keep the strategy test window short, but long enough to observe.
        Mockito.doReturn(1).when(TestcontainersConfiguration.getInstance()).getClientPingTimeout();

        // A port where nothing is listening, so the strategy keeps polling until it times out.
        int closedPort;
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            closedPort = serverSocket.getLocalPort();
        }

        DockerClientProviderStrategy strategy = new DockerClientProviderStrategy() {
            @Override
            public TransportConfig getTransportConfig() {
                return TransportConfig.builder().dockerHost(URI.create("tcp://localhost:" + closedPort)).build();
            }

            @Override
            public String getDescription() {
                return "strategy pointing at a closed port";
            }
        };

        Thread.UncaughtExceptionHandler original = Thread.getDefaultUncaughtExceptionHandler();
        Thread.UncaughtExceptionHandler sentinel = (thread, throwable) -> {};
        Thread.setDefaultUncaughtExceptionHandler(sentinel);

        // Awaitility installs its own default uncaught exception handler for the duration of the await and
        // restores the previous one afterwards, so the replacement is only observable while the await runs.
        AtomicBoolean handlerReplaced = new AtomicBoolean(false);
        AtomicBoolean stopWatching = new AtomicBoolean(false);
        Thread watcher = new Thread(() -> {
            while (!stopWatching.get()) {
                if (Thread.getDefaultUncaughtExceptionHandler() != sentinel) {
                    handlerReplaced.set(true);
                    return;
                }
            }
        });
        watcher.setDaemon(true);

        try {
            watcher.start();
            strategy.test();
        } finally {
            stopWatching.set(true);
            watcher.join();
            Thread.setDefaultUncaughtExceptionHandler(original);
        }

        assertThat(handlerReplaced)
            .as("Testcontainers must not replace the global uncaught exception handler")
            .isFalse();
    }
}

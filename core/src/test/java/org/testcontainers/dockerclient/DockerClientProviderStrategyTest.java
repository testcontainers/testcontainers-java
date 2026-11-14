package org.testcontainers.dockerclient;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.File;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link DockerClientProviderStrategy#test()} against Windows named pipes the test creates itself. A pipe with
 * a single instance that one client holds is busy, as Docker Desktop's pipe is while every instance of it is taken
 * by other processes, so the busy state needs no race to reproduce.
 */
class DockerClientProviderStrategyTest {

    private static final int PIPE_ACCESS_DUPLEX = 0x00000003;

    private static final int PIPE_NOWAIT = 0x00000001;

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testPassesWhenNamedPipeIsBusyAndFreesWithinPingTimeout() throws Exception {
        String pipePath = uniquePipePath();
        Pointer pipe = Kernel32.INSTANCE.CreateNamedPipe(pipePath, PIPE_ACCESS_DUPLEX, PIPE_NOWAIT, 1, 0, 0, 0, null);
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try (
            FileChannel holder = FileChannel.open(
                Paths.get(pipePath),
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
            )
        ) {
            assertThat(new File(pipePath).exists()).as("File.exists() on the busy pipe").isFalse();

            ScheduledFuture<?> release = executor.schedule(
                () -> {
                    holder.close();
                    Kernel32.INSTANCE.DisconnectNamedPipe(pipe);
                    Kernel32.INSTANCE.ConnectNamedPipe(pipe, null);
                    return null;
                },
                500,
                TimeUnit.MILLISECONDS
            );

            assertThat(strategyFor(pipePath).test()).isTrue();
            release.get();
        } finally {
            executor.shutdownNow();
            Kernel32.INSTANCE.CloseHandle(pipe);
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void testFailsWhenNamedPipeDoesNotExist() {
        assertThat(strategyFor(uniquePipePath()).test()).isFalse();
    }

    private static String uniquePipePath() {
        return "//./pipe/testcontainers-" + UUID.randomUUID();
    }

    private static DockerClientProviderStrategy strategyFor(String pipePath) {
        return new DockerClientProviderStrategy() {
            @Override
            public String getDescription() {
                return "named pipe " + pipePath;
            }

            @Override
            public TransportConfig getTransportConfig() {
                return TransportConfig.builder().dockerHost(URI.create("npipe://" + pipePath)).build();
            }
        };
    }

    interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class, W32APIOptions.DEFAULT_OPTIONS);

        Pointer CreateNamedPipe(
            String name,
            int openMode,
            int pipeMode,
            int maxInstances,
            int outBufferSize,
            int inBufferSize,
            int defaultTimeOut,
            Pointer securityAttributes
        );

        boolean ConnectNamedPipe(Pointer pipe, Pointer overlapped);

        boolean DisconnectNamedPipe(Pointer pipe);

        boolean CloseHandle(Pointer handle);
    }
}

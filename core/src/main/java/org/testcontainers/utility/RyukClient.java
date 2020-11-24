package org.testcontainers.utility;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Frame;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.rnorth.ducttape.unreliables.Unreliables;

import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@RequiredArgsConstructor
@Slf4j
class RyukClient implements AutoCloseable {

    private final DockerClient dockerClient;

    private final String ryukContainerId;

    private final PipedOutputStream outputStream = new PipedOutputStream();

    private final AtomicBoolean acked = new AtomicBoolean(true);

    private ResultCallback.Adapter<Frame> listener;

    @SneakyThrows
    public void connect() {
        ExecCreateCmdResponse exec = dockerClient.execCreateCmd(ryukContainerId)
            .withAttachStdin(true)
            .withAttachStdout(true)
            .withCmd("nc", "localhost", "8080")
            .exec();

        listener = dockerClient.execStartCmd(exec.getId())
            .withStdIn(new PipedInputStream(outputStream))
            .exec(new ResultCallback.Adapter<Frame>() {
                @Override
                @SneakyThrows
                public void onNext(Frame object) {
                    String payload = new String(object.getPayload(), StandardCharsets.UTF_8);
                    if ("ACK".equals(payload.trim())) {
                        acked.set(true);
                    }
                }
            });
        listener.awaitStarted(15, TimeUnit.SECONDS);
    }

    @SneakyThrows
    public synchronized void acknowledge(String query) {
        if (!acked.compareAndSet(true, false)) {
            throw new IllegalStateException("ACK is in progress");
        }
        log.debug("Sending '{}' to Ryuk", query);
        outputStream.write((query + "\n").getBytes());
        outputStream.flush();

        Unreliables.retryUntilTrue(5, TimeUnit.SECONDS, acked::get);

        log.debug("Received 'ACK' from Ryuk");
    }

    @Override
    @SneakyThrows
    public void close() {
        if (listener != null) {
            listener.close();
        }
    }
}

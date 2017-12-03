package org.testcontainers.containers.wait;

import com.google.common.util.concurrent.Uninterruptibles;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * TODO: Javadocs
 */
@RequiredArgsConstructor @Slf4j
public class SocketCloseWaiter {

    private final String address;
    private final Set<Integer> ports;

    private final List<Socket> sockets = new ArrayList<>();

    public void start() {
        for (Integer port : ports) {
            try {
                final Socket socket = new Socket(address, port);
                sockets.add(socket);
            } catch (IOException ignored) {
            }
        }
    }

    public void waitUntilAllClosed(int i, TimeUnit unit) throws TimeoutException {
        final long deadline = System.currentTimeMillis() + unit.toMillis(i);
        for (Socket socket : sockets) {
            try {
                while (socket.getInputStream().read() != -1) {
                    log.debug("Waiting for socket to be closed by server: {}", socket);
                    if (System.currentTimeMillis() > deadline) {
                        throw new TimeoutException();
                    }
                    Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
                }
            } catch (IOException ignored) {
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}

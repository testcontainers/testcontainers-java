package org.testcontainers.containers;

import com.google.common.io.Closer;
import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * TODO: Javadocs
 */
public class DockerPortCollisionTest {

    private Closer closer = Closer.create();

    @Test
    public void doTest() throws Exception {

        final ServerSocket serverSocket = new ServerSocket(0);
        final int port = serverSocket.getLocalPort();
        serverSocket.close();

        for (int i = 0; i < 100; i++) {
            try (GenericContainer c = new FixedHostPortGenericContainer("tutum/hello-world:latest")
                    .withFixedExposedPort(port, 80)) {

                c.start();

                final Socket socket = new Socket(c.getContainerIpAddress(), port);

                System.out.printf("%d: %d\n", i, port);

                final OutputStream outputStream = socket.getOutputStream();
                final InputStream inputStream = socket.getInputStream();

                new Thread(() -> {
                    while (!socket.isClosed()) {
                        try {
                            outputStream.write(0);
                            Uninterruptibles.sleepUninterruptibly(10, TimeUnit.MILLISECONDS);
                        } catch (IOException ignored) {

                        }
                    }
                }).start();

                new Thread(() -> {
                    while (!socket.isClosed()) {
                        try {
                            inputStream.read();
                        } catch (IOException ignored) {

                        }
                    }
                }).start();

                closer.register(socket);
            }
        }

    }

    @After
    public void tearDown() throws Exception {
        closer.close();
    }
}

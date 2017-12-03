package org.testcontainers.containers;

import com.google.common.io.Closer;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * TODO: Javadocs
 */
public class DockerPortCollisionTest {

    private Closer closer = Closer.create();

    @Test
    public void doTest() throws Exception {
        for (int i = 0; i < 100; i++) {
            try (GenericContainer c = new GenericContainer<>("tutum/hello-world:latest")
                    .withExposedPorts(80)) {

                c.start();

                final Socket socket = new Socket(c.getContainerIpAddress(), c.getFirstMappedPort());

                System.out.printf("%d: %d\n", i, c.getFirstMappedPort());

                final OutputStream stream = socket.getOutputStream();

                new Thread(() -> {
                    while (! socket.isClosed()) {
                        try {
                            stream.write(0);
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

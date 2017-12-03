package org.testcontainers.containers;

import com.google.common.io.Closer;
import org.junit.After;
import org.junit.Test;

import java.net.Socket;

/**
 * TODO: Javadocs
 */
public class DockerPortCollisionTest {

    private Closer closer = Closer.create();

    @Test
    public void doTest() throws Exception {
        for (int i = 0; i < 50; i++) {
            try (GenericContainer c = new GenericContainer<>("tutum/hello-world:latest")
                    .withExposedPorts(80)) {

                c.start();

                final Socket socket = new Socket(c.getContainerIpAddress(), c.getFirstMappedPort());
                closer.register(socket);

                Thread.sleep(1L);
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        closer.close();
    }
}

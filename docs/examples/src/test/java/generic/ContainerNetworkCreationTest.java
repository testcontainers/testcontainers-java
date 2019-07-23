package generic;

import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;

import static org.junit.Assert.assertEquals;

public class ContainerNetworkCreationTest {

    @Test
    public void testContainersWithCustomNetwork() throws Exception {

        // useCustomNetwork {
        final Network network = Network.newNetwork();

        final GenericContainer server = new GenericContainer()
            .withNetwork(network)
            .withNetworkAliases("server")
            .withCommand("/bin/sh", "-c", "while true ; do printf 'HTTP/1.1 200 OK\\n\\nyay' | nc -l -p 8080; done");

        final GenericContainer container = new GenericContainer()
            .withNetwork(network)
            .withCommand("top");
        
        server.start();
        container.start();

        // Validate network communication works
        final String response = container.execInContainer("wget", "-O", "-", "http://server:8080").getStdout();
        assertEquals("received response", "yay", response);
        // }

        server.close();
        container.close();
        network.close();
    }
}

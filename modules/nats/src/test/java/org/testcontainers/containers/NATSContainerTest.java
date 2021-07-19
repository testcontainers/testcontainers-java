package org.testcontainers.containers;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.classic.methods.HttpGet;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpResponse;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.SneakyThrows;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class NATSContainerTest {
    public static final String NATS_PROTOCOL = "nats://";
    private static final String NATS_NON_DEFAULT_IMAGE = "nats:2.1-alpine3.10";

    @Test
    public void testNATSServerNonDefaultImage() {
        try (NATSContainer container = new NATSContainer(NATS_NON_DEFAULT_IMAGE)) {
            container.start();

            assertConnectionStatus(container);
        }
    }

    @Test
    @SneakyThrows({IOException.class, InterruptedException.class})
    public void testNATSClientConnectionTest() {
        // natsContainerUsage {
        // Create a NATS container.
        NATSContainer container = new NATSContainer();

        // Start the container. This step might take some time...
        container.start();

        // Connect to NATS
        Connection connection = Nats.connect(
            new io.nats.client.Options.Builder().server(
                "nats://" + container.getHost() + ":" + container.getFirstMappedPort()
            ).build());

        // Do whatever you want with the NATS connection ...
        assertThat(connection.getStatus(), equalTo(Connection.Status.CONNECTED));

        // Stop the container.
        container.close();
        // }
    }

    @Test
    public void testNATSServerStatus() throws IOException {
        try (NATSContainer container = new NATSContainer()) {
            container.start();

            HttpUriRequest request = new HttpGet(getUri(container));
            HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
            assertThat(httpResponse.getCode(), equalTo(HttpStatus.SC_OK));
        }
    }

    private String getUri(NATSContainer container) {
        return "http://" + container.getHost() + ":" + container.getMappedPort(NATSContainer.NATS_MGMT_PORT) + "/varz";
    }

    private void assertConnectionStatus(NATSContainer container) {
        Connection c = getConnection(container);
        assertThat(c.getStatus(), equalTo(Connection.Status.CONNECTED));
    }

    @SneakyThrows({IOException.class, InterruptedException.class})
    public static Connection getConnection(ContainerState containerState) {

        Options options = new io.nats.client.Options.Builder().
            server(NATS_PROTOCOL + containerState.getHost() + ":" + containerState.getFirstMappedPort()).
            build();

        return Nats.connect(options);
    }
}

package com.example;

import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class NatsContainerTest {

    public static final Integer NATS_PORT = 4222;

    public static final Integer NATS_MGMT_PORT = 8222;

    @Test
    void test() throws IOException, InterruptedException {
        try (
            GenericContainer<?> nats = new GenericContainer<>("nats:2.9.8-alpine3.16")
                .withExposedPorts(NATS_PORT, NATS_MGMT_PORT)
        ) {
            nats.start();

            Connection connection = Nats.connect(
                new Options.Builder().server("nats://" + nats.getHost() + ":" + nats.getMappedPort(NATS_PORT)).build()
            );

            assertThat(connection.getStatus()).isEqualTo(Connection.Status.CONNECTED);
        }
    }

    @Test
    void testServerStatus() throws IOException {
        try (
            GenericContainer<?> nats = new GenericContainer<>("nats:2.9.8-alpine3.16")
                .withExposedPorts(NATS_PORT, NATS_MGMT_PORT)
        ) {
            nats.start();

            HttpUriRequest request = new HttpGet(
                String.format("http://%s:%d/varz", nats.getHost(), nats.getMappedPort(NATS_MGMT_PORT))
            );
            HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);

            assertThat(httpResponse.getStatusLine().getStatusCode()).isEqualTo(HttpStatus.SC_OK);
        }
    }
}

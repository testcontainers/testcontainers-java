package org.testcontainers.containers;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.classic.methods.HttpGet;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpResponse;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import io.nats.client.Connection;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class NATSContainerTest {

    private static final String NATS_NON_DEFAULT_IMAGE = "nats:2.1-alpine3.10";
    private static final String NATS_MANAGEMENT_VARZ_URL = "http://localhost:8222/varz";


    @Test
    public void testNATSServerNonDefaultImage() throws IOException {
        try (NATSContainer container = new NATSContainer(NATS_NON_DEFAULT_IMAGE)) {
            container.start();

            assertConnectionStatus(container);
        }
    }

    @Test
    public void testNATSClientConnectionTest() throws IOException {
        try (NATSContainer container = new NATSContainer()) {
            container.start();

            assertConnectionStatus(container);
        }
    }

    @Test
    public void testNATSServerStatus() throws IOException {
        try (NATSContainer container = new NATSContainer()) {
            container.start();

            HttpUriRequest request = new HttpGet(NATS_MANAGEMENT_VARZ_URL);
            HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
            assertThat(httpResponse.getCode(), equalTo(HttpStatus.SC_OK));
        }
    }

    private void assertConnectionStatus(NATSContainer container) throws IOException {
        Connection c = NATSContainer.getConnection(container);
        assertThat(c.getStatus(), equalTo(Connection.Status.CONNECTED));
    }

}

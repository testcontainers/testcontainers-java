package org.testcontainers.hivemq;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

class ContainerWithControlCenterIT {

    public static final int CONTROL_CENTER_PORT = 8080;

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        try (
            final HiveMQContainer hivemq = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("4.7.4"))
                .withControlCenter()
        ) {
            hivemq.start();

            try (final CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
                final HttpUriRequest request = new HttpGet(
                    "http://" + hivemq.getHost() + ":" + hivemq.getMappedPort(CONTROL_CENTER_PORT)
                );
                httpClient.execute(request);
            }
        }
    }
}

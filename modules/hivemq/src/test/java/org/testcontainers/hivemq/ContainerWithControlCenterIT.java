package org.testcontainers.hivemq;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

public class ContainerWithControlCenterIT {

    public static final int CONTROL_CENTER_PORT = 8080;

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void test() throws Exception {

        try (final HiveMQContainer hivemq =
                 new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_EE_IMAGE_NAME)
                     .withControlCenter()) {

            hivemq.start();

            final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            final HttpUriRequest request = new HttpGet("http://" + hivemq.getHost() + ":" + hivemq.getMappedPort(CONTROL_CENTER_PORT));
            httpClient.execute(request);

            hivemq.stop();
        }

    }
}

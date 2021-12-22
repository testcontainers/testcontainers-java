package org.testcontainers.hivemq;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;

/**
 * @author Yannick Weber
 */
public class ContainerWithControlCenterIT {

    public static final int CONTROL_CENTER_PORT = 8080;

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void test() throws Exception {

        final HiveMQContainer extension =
                new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_EE_IMAGE_NAME)
                        .withControlCenter();

        extension.start();

        final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        final HttpUriRequest request = new HttpGet("http://localhost:" + extension.getMappedPort(CONTROL_CENTER_PORT));
        httpClient.execute(request);

        extension.stop();
    }
}

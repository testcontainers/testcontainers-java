package org.testcontainers.junit;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

/**
 * Simple test case / demonstration of creating a fresh container image from a Dockerfile DSL when the test
 * is parameterized.
 */
@RunWith(Parameterized.class)
public class ParameterizedDockerfileContainerTest {

    @Parameterized.Parameters(name = "{0}")
    public static Object[] data() {
        return new Object[] { "alpine:3.2", "alpine:3.3" };
    }

    public ParameterizedDockerfileContainerTest(String baseImage) {
        container = new GenericContainer(new ImageFromDockerfile().withDockerfileFromBuilder(builder -> {
                builder
                        .from(baseImage)
                        .run("apk add --update nginx")
                        .cmd("nginx", "-g", "daemon off;")
                        .build();
            })).withExposedPorts(80);
    }

    @Rule
    public GenericContainer container;

    @Test
    public void simpleTest() throws IOException {
        String address = String.format("http://%s:%s", container.getContainerIpAddress(), container.getMappedPort(80));

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(address);

        Unreliables.retryUntilSuccess(5, TimeUnit.SECONDS, () -> {
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                assertEquals("A container built from a dockerfile can run nginx as expected, and returns a good status code",
                                200,
                                response.getStatusLine().getStatusCode());
                assertTrue("A container built from a dockerfile can run nginx as expected, and returns an expected Server header",
                                response.getHeaders("Server")[0].getValue().contains("nginx"));
            }
            return true;
        });
    }
}

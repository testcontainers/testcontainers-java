package org.testcontainers.junit;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple test case / demonstration of creating a fresh container image from a Dockerfile DSL
 */
class DockerfileContainerTest {

    @Test
    void simpleDslTest() throws IOException {
        try (
            GenericContainer<?> dslContainer = new GenericContainer<>(
                new ImageFromDockerfile("tcdockerfile/nginx", false)
                    .withDockerfileFromBuilder(builder -> {
                        builder
                            .from("alpine:3.2") //
                            .run("apk add --update nginx")
                            .cmd("nginx", "-g", "daemon off;")
                            .build();
                    })
            )
        ) {
            dslContainer.withExposedPorts(80);
            dslContainer.start();

            String address = String.format("http://%s:%s", dslContainer.getHost(), dslContainer.getMappedPort(80));
            HttpGet get = new HttpGet(address);

            try (
                CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                CloseableHttpResponse response = httpClient.execute(get)
            ) {
                assertThat(response.getStatusLine().getStatusCode())
                    .as("A container built from a dockerfile can run nginx as expected, and returns a good status code")
                    .isEqualTo(200);
                assertThat(response.getHeaders("Server")[0].getValue())
                    .as(
                        "A container built from a dockerfile can run nginx as expected, and returns an expected Server header"
                    )
                    .contains("nginx");
            }
        }
    }
}

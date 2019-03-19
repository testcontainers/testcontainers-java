package org.testcontainers.junit.jupiter;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class ComposeContainerTests {

    @Container
    private DockerComposeContainer composeContainer = new DockerComposeContainer(
        new File("src/test/resources/docker-compose.yml"))
        .withExposedService("whoami_1", 80, Wait.forHttp("/"));

    private String host;

    private int port;

    @BeforeEach
    void setup() {
        host = composeContainer.getServiceHost("whoami_1", 80);
        port = composeContainer.getServicePort("whoami_1", 80);
    }

    @Test
    void running_compose_defined_container_is_accessible_on_configured_port() throws Exception {
        HttpClient client = HttpClientBuilder.create().build();

        HttpResponse response = client.execute(new HttpGet("http://" + host + ":" + port));

        assertEquals(200, response.getStatusLine().getStatusCode());
    }
}

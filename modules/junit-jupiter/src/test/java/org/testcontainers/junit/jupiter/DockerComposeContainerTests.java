package org.testcontainers.junit.jupiter;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class DockerComposeContainerTests {

    @Container
    private DockerComposeContainer composeContainer = new DockerComposeContainer(
        new File("src/test/resources/docker-compose.yml")
    )
        .withExposedService("whoami_1", 80, Wait.forHttp("/"));

    @Test
    void running_compose_defined_container_is_accessible_on_configured_port() throws Exception {
        HttpClient client = HttpClientBuilder.create().build();

        String host = composeContainer.getServiceHost("whoami_1", 80);
        int port = composeContainer.getServicePort("whoami_1", 80);

        HttpResponse response = client.execute(new HttpGet("http://" + host + ":" + port));

        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
    }
}

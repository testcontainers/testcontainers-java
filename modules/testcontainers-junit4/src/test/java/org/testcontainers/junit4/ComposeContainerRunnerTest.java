package org.testcontainers.junit4;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(TestContainersRunner.class)
public class ComposeContainerRunnerTest {

    @Container
    public DockerComposeContainer composeContainer = new DockerComposeContainer(
        new File("src/test/resources/docker-compose.yml")
    )
        .withExposedService("whoami_1", 80, Wait.forHttp("/"));

    private String host;

    private int port;

    @Before
    public void setup() {
        host = composeContainer.getServiceHost("whoami_1", 80);
        port = composeContainer.getServicePort("whoami_1", 80);
    }

    @Test
    public void running_compose_defined_container_is_accessible_on_configured_port() throws Exception {
        HttpClient client = HttpClientBuilder.create().build();

        HttpResponse response = client.execute(new HttpGet("http://" + host + ":" + port));

        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
    }
}

package org.testcontainers.containers;

import org.junit.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class MockServerContainerTest {

    public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName.parse(
        "jamesdbloom/mockserver:mockserver-5.5.4"
    );

    @Test
    public void shouldCallActualMockserverVersion() throws Exception {
        String actualVersion = MockServerClient.class.getPackage().getImplementationVersion();
        try (
            MockServerContainer mockServer = new MockServerContainer(
                MOCKSERVER_IMAGE.withTag("mockserver-" + actualVersion)
            )
        ) {
            mockServer.start();

            String expectedBody = "Hello World!";

            MockServerClient client = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());

            assertThat(client.isRunning()).as("Mockserver running").isTrue();

            client.when(request().withPath("/hello")).respond(response().withBody(expectedBody));

            assertThat(SimpleHttpClient.responseFromMockserver(mockServer, "/hello"))
                .as("MockServer returns correct result")
                .isEqualTo(expectedBody);
        }
    }

    @Test
    public void newVersionStartsWithDefaultWaitStrategy() {
        DockerImageName dockerImageName = DockerImageName.parse("mockserver/mockserver:mockserver-5.11.2");
        try (MockServerContainer mockServer = new MockServerContainer(dockerImageName)) {
            mockServer.start();
        }
    }
}

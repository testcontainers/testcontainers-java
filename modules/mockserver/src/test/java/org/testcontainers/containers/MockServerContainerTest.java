package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

public class MockServerContainerTest {

    public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName
        .parse("mockserver/mockserver")
        .withTag("mockserver-" + MockServerClient.class.getPackage().getImplementationVersion());

    @Test
    public void shouldCallActualMockserverVersion() throws Exception {
        try (MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE)) {
            mockServer.start();

            String expectedBody = "Hello World!";

            try (MockServerClient client = new MockServerClient(mockServer.getHost(), mockServer.getServerPort())) {
                assertThat(client.hasStarted()).as("Mockserver running").isTrue();

                client.when(request().withPath("/hello")).respond(response().withBody(expectedBody));

                assertThat(SimpleHttpClient.responseFromMockserver(mockServer, "/hello"))
                    .as("MockServer returns correct result")
                    .isEqualTo(expectedBody);
            }
        }
    }

    @Test
    public void shouldCallMockserverUsingTlsProtocol() throws Exception {
        try (MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE)) {
            mockServer.start();

            String expectedBody = "Hello World!";

            try (
                MockServerClient client = new MockServerClient(mockServer.getHost(), mockServer.getServerPort())
                    .withSecure(true)
            ) {
                assertThat(client.hasStarted()).as("Mockserver running").isTrue();

                client.when(request().withPath("/hello")).respond(response().withBody(expectedBody));

                assertThat(SimpleHttpClient.secureResponseFromMockserver(mockServer, "/hello"))
                    .as("MockServer returns correct result")
                    .isEqualTo(expectedBody);
            }
        }
    }

    @Test
    public void shouldCallMockserverUsingMutualTlsProtocol() throws Exception {
        try (
            MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE)
                .withEnv("MOCKSERVER_TLS_MUTUAL_AUTHENTICATION_REQUIRED", "true")
        ) {
            mockServer.start();

            String expectedBody = "Hello World!";

            try (
                MockServerClient client = new MockServerClient(mockServer.getHost(), mockServer.getServerPort())
                    .withSecure(true)
            ) {
                assertThat(client.hasStarted()).as("Mockserver running").isTrue();

                client.when(request().withPath("/hello")).respond(response().withBody(expectedBody));

                assertThat(SimpleHttpClient.secureResponseFromMockserver(mockServer, "/hello"))
                    .as("MockServer returns correct result")
                    .isEqualTo(expectedBody);
            }
        }
    }

    @Test
    public void newVersionStartsWithDefaultWaitStrategy() {
        try (MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE)) {
            mockServer.start();
        }
    }
}

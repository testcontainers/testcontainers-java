package org.testcontainers.containers;

import org.junit.Test;
import org.mockserver.client.ClientException;
import org.mockserver.client.MockServerClient;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;

public class MockServerContainerTest {

    public static final DockerImageName MOCKSERVER_IMAGE = DockerImageName.parse("jamesdbloom/mockserver:mockserver-5.5.4");

    @Test
    public void shouldCallActualMockserverVersion() throws Exception {
        String actualVersion = MockServerClient.class.getPackage().getImplementationVersion();
        try (MockServerContainer mockServer = new MockServerContainer(MOCKSERVER_IMAGE.withTag("mockserver-" + actualVersion))) {
            mockServer.start();

            String expectedBody = "Hello World!";

            MockServerClient client = new MockServerClient(mockServer.getHost(), mockServer.getServerPort());

            assertThat("Mockserver running", client.isRunning(), is(true));

            client
                .when(request().withPath("/hello"))
                .respond(response().withBody(expectedBody));

            assertThat("MockServer returns correct result",
                SimpleHttpClient.responseFromMockserver(mockServer, "/hello"),
                equalTo(expectedBody)
            );
        }
    }

    @Test
    public void newVersionStartsWithDefaultWaitStrategy() {
        DockerImageName dockerImageName = DockerImageName.parse("mockserver/mockserver").withTag("mockserver-5.11.2");
        try (MockServerContainer mockServer = new MockServerContainer(dockerImageName)) {
            mockServer.start();

            assertThatThrownBy(() -> new MockServerClient(mockServer.getHost(), mockServer.getServerPort()).isRunning())
                .isInstanceOf(ClientException.class)
                .hasMessageContaining("does not match server version \"5.11.2\"");
        }
    }
}

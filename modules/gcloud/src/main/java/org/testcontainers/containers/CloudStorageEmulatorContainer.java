package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Testcontainers implementation for Google Cloud Storage.
 * Default port: 4443
 * <p>
 * Supported image: {@code hub.docker.com/r/fsouza/fake-gcs-server}
 * <p>
 * @see <a href="https://github.com/fsouza/fake-gcs-server">fsouza/fake-gcs-server on GitHub</a>
 */
public class CloudStorageEmulatorContainer extends GenericContainer<CloudStorageEmulatorContainer> {

    public static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("fsouza/fake-gcs-server");

    public static final int EMULATOR_PORT = 4443;

    public CloudStorageEmulatorContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public CloudStorageEmulatorContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE);
        addExposedPorts(EMULATOR_PORT);
        withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("/bin/fake-gcs-server", "-scheme", "http"));
        waitingFor(
            new WaitStrategy() {
                @Override
                public void waitUntilReady(WaitStrategyTarget target) {
                    updateFakeGcsExternalUrl(getEmulatorHttpEndpoint());
                }

                @Override
                public WaitStrategy withStartupTimeout(Duration startupTimeout) {
                    return getWaitStrategy().withStartupTimeout(startupTimeout);
                }
            }
        );
    }

    public String getEmulatorHttpEndpoint() {
        return String.format("http://%s:%d", getHost(), getMappedPort(EMULATOR_PORT));
    }

    private static void updateFakeGcsExternalUrl(String gcsUrl) {
        String json = String.format("{ \"externalUrl\": \"%s\" }", gcsUrl);

        HttpRequest req = HttpRequest
            .newBuilder()
            .uri(URI.create(gcsUrl + "/_internal/config"))
            .header("Content-Type", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .build();

        try {
            HttpResponse<Void> response = HttpClient
                .newBuilder()
                .build()
                .send(req, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() != 200) {
                throw new RuntimeException(
                    "error updating fake-gcs-server with external url, response status code " +
                    response.statusCode() +
                    " != 200"
                );
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

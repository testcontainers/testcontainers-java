package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Testcontainers proxy container for the Docker Model Runner service
 * provided by Docker Desktop.
 * <p>
 * Supported images: {@code alpine/socat}
 * <p>
 * Exposed ports: 80
 */
@Slf4j
public class DockerModelRunnerContainer extends SocatContainer {

    private static final String MODEL_RUNNER_ENDPOINT = "model-runner.docker.internal";

    private static final int PORT = 80;

    private String model;

    public DockerModelRunnerContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public DockerModelRunnerContainer(DockerImageName image) {
        super(image);
        withTarget(PORT, MODEL_RUNNER_ENDPOINT);
        waitingFor(Wait.forHttp("/").forResponsePredicate(res -> res.contains("The service is running")));
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        if (this.model != null) {
            logger().info("Pulling model: {}. Please be patient.", this.model);

            String url = getBaseEndpoint() + "/models/create";
            String payload = String.format("{\"from\": \"%s\"}", this.model);

            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(payload.getBytes());
                    os.flush();
                }

                try (
                    BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                    )
                ) {
                    while (br.readLine() != null) {}
                }
                connection.disconnect();
            } catch (IOException e) {
                logger().error("Failed to pull model {}: {}", this.model, e);
            }
            logger().info("Finished pulling model: {}", this.model);
        }
    }

    public DockerModelRunnerContainer withModel(String model) {
        this.model = model;
        return this;
    }

    public String getBaseEndpoint() {
        return "http://" + getHost() + ":" + getMappedPort(PORT);
    }

    public String getOpenAIEndpoint() {
        return getBaseEndpoint() + "/engines";
    }
}

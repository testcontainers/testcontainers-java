package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DockerModelRunnerContainer extends GenericContainer<DockerModelRunnerContainer> {

    public static final String MODEL_RUNNER_ENDPOINT = "model-runner.docker.internal";

    private SocatContainer socat;

    private String model;

    @Override
    public void start() {
        this.socat =
            new SocatContainer()
                .withTarget(80, MODEL_RUNNER_ENDPOINT)
                .waitingFor(Wait.forHttp("/").forResponsePredicate(res -> res.contains("The service is running")));
        this.socat.start();
        pullModel();
    }

    private void pullModel() {
        logger().info("Pulling model: {}. Please be patient, no progress bar yet!", this.model);
        try {
            String json = String.format("{\"from\":\"%s\"}", this.model);
            String endpoint = "http://" + this.socat.getHost() + ":" + this.socat.getMappedPort(80) + "/models/create";

            URL url = new URL(endpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = json.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            try (
                BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)
                )
            ) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                logger().info(response.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger().info("Finished pulling model: {}", model);
    }

    @Override
    public void stop() {
        this.socat.stop();
    }

    public String getBaseEndpoint() {
        return "http://" + this.socat.getHost() + ":" + this.socat.getMappedPort(80);
    }

    public String getOpenAIEndpoint() {
        return getBaseEndpoint() + "/engines";
    }

    public DockerModelRunnerContainer withModel(String modelName) {
        this.model = modelName;
        return this;
    }
}

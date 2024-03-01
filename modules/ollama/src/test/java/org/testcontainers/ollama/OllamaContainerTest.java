package org.testcontainers.ollama;

import org.junit.Test;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class OllamaContainerTest {

    @Test
    public void containerStart() {
        try ( // container {
            OllamaContainer ollamaContainer = new OllamaContainer("ollama/ollama:0.1.26")
            // }
        ) {
            ollamaContainer.start();

            String version = given()
                .baseUri(ollamaContainer.getEndpoint())
                .get("/api/version")
                .jsonPath()
                .get("version");
            assertThat(version).isEqualTo("0.1.26");
        }
    }

    @Test
    public void downloadModelAndCreateImage() throws IOException, InterruptedException {
        String newImageName = "tc-ollama-allminilm-" + Base58.randomString(4).toLowerCase();
        try (OllamaContainer ollama = new OllamaContainer("ollama/ollama:0.1.26")) {
            ollama.start();
            ollama.execInContainer("ollama", "pull", "all-minilm");

            String modelName = given()
                .baseUri(ollama.getEndpoint())
                .get("/api/tags")
                .jsonPath()
                .getString("models[0].name");
            assertThat(modelName).contains("all-minilm");
            ollama.createImage(newImageName);
        }
        try (
            OllamaContainer ollama = new OllamaContainer(
                DockerImageName.parse(newImageName).asCompatibleSubstituteFor("ollama/ollama")
            )
        ) {
            ollama.start();
            String modelName = given()
                .baseUri(ollama.getEndpoint())
                .get("/api/tags")
                .jsonPath()
                .getString("models[0].name");
            assertThat(modelName).contains("all-minilm");
        }
    }
}

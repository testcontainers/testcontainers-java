package org.testcontainers.ollama;

import org.junit.Test;

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
}

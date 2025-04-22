package org.testcontainers.containers;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

public class DockerModelRunnerContainerTest {

    @Test
    public void pullsModelAndExposesInference() {
        assumeThat(System.getenv("CI")).isNull();

        String modelName = "ai/smollm2:360M-Q4_K_M";

        try (
            DockerModelRunnerContainer dmr = new DockerModelRunnerContainer("alpine/socat:1.7.4.3-r0")
                .withModel(modelName)
        ) {
            dmr.start();

            Response response = RestAssured.get(dmr.getBaseEndpoint() + "/models").thenReturn();
            assertThat(response.body().jsonPath().getList("tags.flatten()")).contains(modelName);

            Response openAiResponse = RestAssured.get(dmr.getOpenAIEndpoint() + "/v1/models").prettyPeek().thenReturn();
            assertThat(openAiResponse.body().jsonPath().getList("data.id")).contains(modelName);
        }
    }
}

package org.testcontainers.nvidia;

import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

public class NimContainerTest {

    @Test
    public void withDefaultConfig() {
        try ( // container {
            NimContainer nim = new NimContainer("nvcr.io/nim/meta/llama3-8b-instruct:1.0.0")
                .withNgcApiKey(System.getenv("NGC_API_KEY"))
            // }
        ) {
            nim.start();

            String id = given().baseUri(nim.getEndpoint()).get("/v1/models").jsonPath().get("data[0].id");
            assertThat(id).isEqualTo("meta/llama3-8b-instruct");
        }
    }
}

package org.testcontainers.chromadb;

import io.restassured.http.ContentType;
import org.junit.Test;

import static io.restassured.RestAssured.given;

public class ChromaDBContainerTest {

    @Test
    public void test() {
        try ( // container {
            ChromaDBContainer chroma = new ChromaDBContainer("chromadb/chroma:0.5.4.dev25")
            // }
        ) {
            chroma.start();

            given()
                .baseUri(chroma.getEndpoint())
                .when()
                .body("{\"name\": \"test\"}")
                .contentType(ContentType.JSON)
                .post("/api/v1/databases")
                .then()
                .statusCode(200);

            given().baseUri(chroma.getEndpoint()).when().get("/api/v1/databases/test").then().statusCode(200);
        }
    }
}

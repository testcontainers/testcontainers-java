package org.testcontainers.chromadb;

import io.restassured.http.ContentType;
import org.junit.Test;

import static io.restassured.RestAssured.given;

public class ChromaDBContainerTest {

    @Test
    public void test() {
        try ( // container {
            ChromaDBContainer chroma = new ChromaDBContainer("chromadb/chroma:0.4.23")
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

    @Test
    public void testVersion2() {
        try (ChromaDBContainer chroma = new ChromaDBContainer("chromadb/chroma:1.0.0")) {
            chroma.start();

            given()
                .baseUri(chroma.getEndpoint())
                .when()
                .body("{\"name\": \"test\"}")
                .contentType(ContentType.JSON)
                .post("/api/v2/tenants")
                .then()
                .statusCode(200);

            given().baseUri(chroma.getEndpoint()).when().get("/api/v2/tenants/test").then().statusCode(200);
        }
    }
}

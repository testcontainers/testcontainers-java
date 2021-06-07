package org.testcontainers.containers.jwt;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class JwtContainerTest {
    private static JwtContainer jwtContainer;

    @BeforeClass
    public static void before() {
        jwtContainer = new JwtContainer();
        jwtContainer.start();
    }

    @AfterClass
    public static void after() {
        jwtContainer.stop();
    }

    @Test
    public void should_serve_the_public_key() {
        URL url = jwtContainer.issuer();

        given()
            .port(url.getPort())
            .when()
            .get(url.getFile())
            .then()
            .body("keys[0].kty", equalTo("RSA"))
            .body("keys[0].n", notNullValue())
            .body("keys[0].e", equalTo("AQAB"))
            .body("keys[0].alg", equalTo("RS256"))
            .body("keys[0].kid", equalTo("test"))
            .body("keys[0].use", equalTo("sig"));
    }
}

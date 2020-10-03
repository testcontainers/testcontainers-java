package org.testcontainers.containers;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class JwtContainerTest {
    private static JwtContainer jwt;

    @BeforeClass
    public static void before() {
        jwt = new JwtContainer();
        jwt.start();
    }

    @AfterClass
    public static void after() {
        jwt.stop();
    }

    @Test
    public void serverServeThePublicKey() {
        URL url = jwt.issuer();

        given()
            .port(url.getPort())
            .when()
            .get(url.getFile())
            .then()
            .body("keys[0].kty", equalTo("RSA"));

    }
}

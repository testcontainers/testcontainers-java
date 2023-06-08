package org.testcontainers.containers;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.specification.RequestSpecification;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.applicationserver.ApplicationServerContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

import static io.restassured.RestAssured.given;

public class LibertyContainerTest {

    private static final DockerImageName libertyImage = DockerImageName.parse(LibertyServerContainer.IMAGE)
        .withTag("23.0.0.3-full-java17-openj9");

    private static ApplicationServerContainer testContainer = new LibertyServerContainer(libertyImage)
        .withArchvies(createDeployment())
        .withAppContextRoot("test/app/service/");

    private static Archive<?> createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
            .addPackage("org.testcontainers.containers.app");
    }

    @BeforeClass
    public static void setup() {
        testContainer.start();
    }

    @AfterClass
    public static void teardown() {
        testContainer.stop();
    }

    @Test
    public void testURLs() {
        String expectedURL, actualURL;

        expectedURL = "http://" + testContainer.getHost() + ":" + testContainer.getMappedPort(LibertyServerContainer.DEFAULT_HTTP_PORT);
        actualURL = testContainer.getBaseURL();
        assertThat(actualURL).isEqualTo(expectedURL);

        expectedURL += "/test/app/service";
        actualURL = testContainer.getApplicationURL();
        assertThat(actualURL).isEqualTo(expectedURL);

        actualURL = testContainer.getReadinessURL();
        assertThat(actualURL).isEqualTo(expectedURL);
    }

    @Test
    public void testRestEndpoint() {
        RequestSpecification request = new RequestSpecBuilder()
            .setBaseUri(testContainer.getApplicationURL())
            .build();

        String expected, actual;

        //Add value to cache
        given(request)
            .header("Content-Type", "text/plain")
            .queryParam("value", "post-it")
        .when()
            .post()
        .then()
            .statusCode(200);

        //Verify value in cache
        expected = "[post-it]";
        actual = given(request)
            .accept("text/plain")
        .when()
            .get("/")
        .then()
            .statusCode(200)
        .extract().body().asString();

        assertThat(actual).isEqualTo(expected);
    }
}

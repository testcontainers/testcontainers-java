package org.testcontainers.consul;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This test shows the pattern to use the ConsulContainer @ClassRule for a junit test. It also has tests that ensure
 * the properties were added correctly by reading from Vault with the CLI and over HTTP.
 */
public class ConsulContainerTest {

    @ClassRule
    public static ConsulContainer<?> consulContainer = new ConsulContainer<>(ConsulTestImages.CONSUL_IMAGE)
        .withPropertyInConsul("config/testing1", "value123")
        .withInitCommand("version");

    @Test
    public void readFirstPropertyPathWithCli() throws IOException, InterruptedException {
        GenericContainer.ExecResult result = consulContainer.execInContainer("consul", "kv", "get", "config/testing1");
        final String output = result.getStdout().replaceAll("\\r?\\n", "");
        assertThat(output, containsString("value123"));
    }

    @Test
    public void readFirstSecretPathOverHttpApi() throws InterruptedException {
        given().
            when().
            get("http://" + getHostAndPort() + "/v1/kv/config/testing1").
            then().
            assertThat().body("[0].Value", equalTo(Base64.getEncoder().encodeToString("value123".getBytes(StandardCharsets.UTF_8))));
    }

    private String getHostAndPort() {
        return consulContainer.getHost() + ":" + consulContainer.getMappedPort(8500);
    }
}
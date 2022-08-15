package org.testcontainers.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import io.restassured.RestAssured;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * This test shows the pattern to use the ConsulContainer @ClassRule for a junit test. It also has tests that ensure
 * the properties were added correctly by reading from Consul with the CLI and over HTTP.
 */
public class ConsulContainerTest {

    @ClassRule
    public static ConsulContainer consulContainer = new ConsulContainer(ConsulTestImages.CONSUL_IMAGE)
        .withConsulCommand("kv put config/testing1 value123");

    @Test
    public void readFirstPropertyPathWithCli() throws IOException, InterruptedException {
        GenericContainer.ExecResult result = consulContainer.execInContainer("consul", "kv", "get", "config/testing1");
        final String output = result.getStdout().replaceAll("\\r?\\n", "");
        MatcherAssert.assertThat(output, CoreMatchers.containsString("value123"));
    }

    @Test
    public void readFirstSecretPathOverHttpApi() throws InterruptedException {
        RestAssured
            .given()
            .when()
            .get("http://" + getHostAndPort() + "/v1/kv/config/testing1")
            .then()
            .assertThat()
            .body(
                "[0].Value",
                CoreMatchers.equalTo(Base64.getEncoder().encodeToString("value123".getBytes(StandardCharsets.UTF_8)))
            );
    }

    @Test
    public void writeAndReadMultipleValuesUsingClient() {
        final ConsulClient consulClient = new ConsulClient(
            consulContainer.getHost(),
            consulContainer.getFirstMappedPort()
        );

        final Map<String, String> properties = new HashMap<>();
        properties.put("value", "world");
        properties.put("other_value", "another world");

        // Write operation
        properties.forEach((key, value) -> {
            Response<Boolean> writeResponse = consulClient.setKVValue(key, value);
            Assertions.assertThat(writeResponse.getValue()).isTrue();
        });

        // Read operation
        properties.forEach((key, value) -> {
            Response<GetValue> readResponse = consulClient.getKVValue(key);
            Assertions.assertThat(readResponse.getValue().getDecodedValue()).isEqualTo(value);
        });
    }

    private String getHostAndPort() {
        return consulContainer.getHost() + ":" + consulContainer.getMappedPort(8500);
    }
}

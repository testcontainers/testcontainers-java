package org.testcontainers.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConsulContainerTest {

    private static ConsulContainer consul = new ConsulContainer("hashicorp/consul:1.15")
        .withConsulCommand("kv put config/testing1 value123");

    @BeforeAll
    static void setup() {
        consul.start();
    }

    @AfterAll
    static void teardown() {
        consul.stop();
    }

    @Test
    void readFirstPropertyPathWithCli() throws IOException, InterruptedException {
        GenericContainer.ExecResult result = consul.execInContainer("consul", "kv", "get", "config/testing1");
        final String output = result.getStdout().replaceAll("\\r?\\n", "");
        assertThat(output).contains("value123");
    }

    @Test
    void readFirstSecretPathOverHttpApi() {
        io.restassured.response.Response response = RestAssured
            .given()
            .when()
            .get("http://" + getHostAndPort() + "/v1/kv/config/testing1")
            .andReturn();

        assertThat(response.body().jsonPath().getString("[0].Value"))
            .isEqualTo(Base64.getEncoder().encodeToString("value123".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void writeAndReadMultipleValuesUsingClient() {
        final ConsulClient consulClient = new ConsulClient(consul.getHost(), consul.getFirstMappedPort());

        final Map<String, String> properties = new HashMap<>();
        properties.put("value", "world");
        properties.put("other_value", "another world");

        // Write operation
        properties.forEach((key, value) -> {
            Response<Boolean> writeResponse = consulClient.setKVValue(key, value);
            assertThat(writeResponse.getValue()).isTrue();
        });

        // Read operation
        properties.forEach((key, value) -> {
            Response<GetValue> readResponse = consulClient.getKVValue(key);
            assertThat(readResponse.getValue().getDecodedValue()).isEqualTo(value);
        });
    }

    private String getHostAndPort() {
        return consul.getHost() + ":" + consul.getMappedPort(8500);
    }
}

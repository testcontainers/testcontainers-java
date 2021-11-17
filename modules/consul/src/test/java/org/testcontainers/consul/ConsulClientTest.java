package org.testcontainers.consul;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.consul.ConsulTestImages.CONSUL_IMAGE;

import java.util.HashMap;
import java.util.Map;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;

import org.junit.Test;

public class ConsulClientTest {

    @Test
    public void writeAndReadMultipleValues() {
        try (
            ConsulContainer<?> consulContainer = new ConsulContainer<>(CONSUL_IMAGE);
        ) {

            consulContainer.start();

            final ConsulClient consulClient = new ConsulClient(consulContainer.getHost(), consulContainer.getFirstMappedPort());

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
    }
}

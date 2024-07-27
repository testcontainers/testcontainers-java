package org.testcontainers.grafana;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.Test;
import uk.org.webcompere.systemstubs.SystemStubs;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class LgtmStackContainerTest {

    @Test
    public void shouldPublishMetric() throws Exception {
        try ( // container {
            LgtmStackContainer lgtm = new LgtmStackContainer("grafana/otel-lgtm:0.6.0")
            // }
        ) {
            lgtm.start();

            String version = RestAssured
                .get(String.format("http://%s:%s/api/health", lgtm.getHost(), lgtm.getMappedPort(3000)))
                .jsonPath()
                .get("version");
            assertThat(version).isEqualTo("11.0.0");

            OtlpConfig otlpConfig = createOtlpConfig(lgtm);
            MeterRegistry meterRegistry = SystemStubs
                .withEnvironmentVariable("OTEL_SERVICE_NAME", "testcontainers")
                .execute(() -> new OtlpMeterRegistry(otlpConfig, Clock.SYSTEM));
            Counter.builder("test.counter").register(meterRegistry).increment(2);

            Awaitility
                .given()
                .pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(5))
                .ignoreExceptions()
                .untilAsserted(() -> {
                    Response response = RestAssured
                        .given()
                        .queryParam("query", "test_counter_total{job=\"testcontainers\"}")
                        .get(String.format("%s/api/v1/query", lgtm.getPromehteusHttpUrl()))
                        .prettyPeek()
                        .thenReturn();
                    assertThat(response.getStatusCode()).isEqualTo(200);
                    assertThat(response.body().jsonPath().getList("data.result[0].value")).contains("2");
                });
        }
    }

    private static OtlpConfig createOtlpConfig(LgtmStackContainer lgtm) {
        return new OtlpConfig() {
            @Override
            public String url() {
                return String.format("%s/v1/metrics", lgtm.getOtlpHttpUrl());
            }

            @Override
            public Duration step() {
                return Duration.ofSeconds(1);
            }

            @Override
            public String get(String s) {
                return null;
            }
        };
    }
}

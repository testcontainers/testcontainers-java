package org.testcontainers.grafana;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.registry.otlp.OtlpConfig;
import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.awaitility.Awaitility;
import org.junit.Test;
import uk.org.webcompere.systemstubs.SystemStubs;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class LgtmStackContainerTest {

    @Test
    public void shouldPublishMetricsTracesAndLogs() throws Exception {
        try ( // container {
            LgtmStackContainer lgtm = new LgtmStackContainer("grafana/otel-lgtm:0.11.1")
            // }
        ) {
            lgtm.start();

            OtlpGrpcSpanExporter spanExporter = OtlpGrpcSpanExporter
                .builder()
                .setTimeout(Duration.ofSeconds(1))
                .setEndpoint(lgtm.getOtlpGrpcUrl())
                .build();

            OtlpGrpcLogRecordExporter logExporter = OtlpGrpcLogRecordExporter
                .builder()
                .setTimeout(Duration.ofSeconds(1))
                .setEndpoint(lgtm.getOtlpGrpcUrl())
                .build();

            BatchSpanProcessor spanProcessor = BatchSpanProcessor
                .builder(spanExporter)
                .setScheduleDelay(500, TimeUnit.MILLISECONDS)
                .build();

            SdkTracerProvider tracerProvider = SdkTracerProvider
                .builder()
                .addSpanProcessor(spanProcessor)
                .setResource(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), "test-service")))
                .build();

            SdkLoggerProvider loggerProvider = SdkLoggerProvider
                .builder()
                .addLogRecordProcessor(SimpleLogRecordProcessor.create(logExporter))
                .build();

            OpenTelemetrySdk openTelemetry = OpenTelemetrySdk
                .builder()
                .setTracerProvider(tracerProvider)
                .setLoggerProvider(loggerProvider)
                .build();

            String version = RestAssured
                .get(String.format("http://%s:%s/api/health", lgtm.getHost(), lgtm.getMappedPort(3000)))
                .jsonPath()
                .get("version");
            assertThat(version).isEqualTo("12.0.0");

            OtlpConfig otlpConfig = createOtlpConfig(lgtm);
            MeterRegistry meterRegistry = SystemStubs
                .withEnvironmentVariable("OTEL_SERVICE_NAME", "testcontainers")
                .execute(() -> new OtlpMeterRegistry(otlpConfig, Clock.SYSTEM));
            Counter.builder("test.counter").register(meterRegistry).increment(2);

            Logger logger = openTelemetry.getSdkLoggerProvider().loggerBuilder("test").build();
            logger
                .logRecordBuilder()
                .setBody("Test log!")
                .setAttribute(AttributeKey.stringKey("job"), "test-job")
                .emit();

            Tracer tracer = openTelemetry.getTracer("test");
            Span span = tracer.spanBuilder("test").startSpan();
            span.end();

            Awaitility
                .given()
                .pollInterval(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(5))
                .ignoreExceptions()
                .untilAsserted(() -> {
                    Response metricResponse = RestAssured
                        .given()
                        .queryParam("query", "test_counter_total{job=\"testcontainers\"}")
                        .get(String.format("%s/api/v1/query", lgtm.getPrometheusHttpUrl()))
                        .prettyPeek()
                        .thenReturn();
                    assertThat(metricResponse.getStatusCode()).isEqualTo(200);
                    assertThat(metricResponse.body().jsonPath().getList("data.result[0].value")).contains("2");

                    Response logResponse = RestAssured
                        .given()
                        .queryParam("query", "{service_name=\"unknown_service:java\"}")
                        .get(String.format("%s/loki/api/v1/query_range", lgtm.getLokiUrl()))
                        .prettyPeek()
                        .thenReturn();
                    assertThat(logResponse.getStatusCode()).isEqualTo(200);
                    assertThat(logResponse.body().jsonPath().getString("data.result[0].values[0][1]"))
                        .isEqualTo("Test log!");

                    Response traceResponse = RestAssured
                        .given()
                        .get(String.format("%s/api/search", lgtm.getTempoUrl()))
                        .prettyPeek()
                        .thenReturn();
                    assertThat(traceResponse.getStatusCode()).isEqualTo(200);
                    assertThat(traceResponse.body().jsonPath().getString("traces[0].rootServiceName"))
                        .isEqualTo("test-service");
                });

            openTelemetry.close();
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

package org.testcontainers.containers;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings;
import com.google.cloud.bigquery.storage.v1.CreateWriteStreamRequest;
import com.google.cloud.bigquery.storage.v1.TableName;
import com.google.cloud.bigquery.storage.v1.WriteStream;
import io.grpc.ManagedChannelBuilder;
import org.threeten.bp.Duration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BigQueryEmulatorContainerTest {

    @Test
    public void testGrcp() throws IOException {
        // Shallow test, validate that connection can be set up, and attempt to create write stream fails.
        // BigQueryWriteSettings requires a HTTP/2 connection, not provided by the originally exposed endpoint. A "not found" exceptionm
        // indicates successful
        try (BigQueryEmulatorContainer container = new BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.6.5")) {
            container.start();
            BigQueryWriteSettings.Builder bigQueryWriteSettingsBuilder = BigQueryWriteSettings.newBuilder();

            bigQueryWriteSettingsBuilder.createWriteStreamSettings()
                .setRetrySettings(bigQueryWriteSettingsBuilder.createWriteStreamSettings()
                    .getRetrySettings()
                    .toBuilder()
                    .setTotalTimeout(Duration.ofSeconds(60))
                    .build());

            BigQueryWriteClient bigQueryWriteClient = BigQueryWriteClient.create(
            bigQueryWriteSettingsBuilder.setTransportChannelProvider(FixedTransportChannelProvider.create(GrpcTransportChannel.create(
                    ManagedChannelBuilder.forAddress(container.getHost(), container.getEmulatorGrpcPort()).usePlaintext().build())))
                .setCredentialsProvider(NoCredentialsProvider.create())
                .build()
            );

            TableName parentTable = TableName.of(container.getProjectId(), "dataset", "table");
            CreateWriteStreamRequest createWriteStreamRequest = CreateWriteStreamRequest.newBuilder()
                .setParent(parentTable.toString())
                .setWriteStream(WriteStream.newBuilder().setType(WriteStream.Type.PENDING))
                .build();

            String message = null;
            try {
                // This will fail, extract error message to check that it fails in a "we reached the backend" way to ensure that setup was correct
                WriteStream writeStream =  bigQueryWriteClient.createWriteStream(createWriteStreamRequest);
                // Example setting up StreamWriter. Note passing bigQueryWriteClient as parameter, this is needed to avoid using gcloud credentials:
                /* StreamWriter writer = StreamWriter.newBuilder(writeStream.getName(), bigQueryWriteClient).setWriterSchema(schema).build(); */
            } catch (RuntimeException e) {
                message = e.getMessage();
            }
            assertThat(message).contains("dataset dataset is not found in project test-project");

            bigQueryWriteClient.shutdown();
            bigQueryWriteClient.close();
        }
    }

    @Test
    void test() throws Exception {
        try (
            // emulatorContainer {
            BigQueryEmulatorContainer container = new BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.4.3")
            // }
        ) {
            container.start();

            // bigQueryClient {
            String url = container.getEmulatorHttpEndpoint();
            BigQueryOptions options = BigQueryOptions
                .newBuilder()
                .setProjectId(container.getProjectId())
                .setHost(url)
                .setLocation(url)
                .setCredentials(NoCredentials.getInstance())
                .build();
            BigQuery bigQuery = options.getService();
            // }

            String fn =
                "CREATE FUNCTION testr(arr ARRAY<STRUCT<name STRING, val INT64>>) AS ((SELECT SUM(IF(elem.name = \"foo\",elem.val,null)) FROM UNNEST(arr) AS elem))";

            bigQuery.query(QueryJobConfiguration.newBuilder(fn).build());

            String sql =
                "SELECT testr([STRUCT<name STRING, val INT64>(\"foo\", 10), STRUCT<name STRING, val INT64>(\"bar\", 40), STRUCT<name STRING, val INT64>(\"foo\", 20)])";
            TableResult result = bigQuery.query(QueryJobConfiguration.newBuilder(sql).build());
            List<BigDecimal> values = result
                .streamValues()
                .map(fieldValues -> fieldValues.get(0).getNumericValue())
                .collect(Collectors.toList());
            assertThat(values).containsOnly(BigDecimal.valueOf(30));
        }
    }
}

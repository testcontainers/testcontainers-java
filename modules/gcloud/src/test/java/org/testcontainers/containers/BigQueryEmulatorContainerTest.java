package org.testcontainers.containers;

import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.DatasetId;
import com.google.cloud.bigquery.DatasetInfo;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.TableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
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

    private BigQuery getBigQuery(BigQueryEmulatorContainer container) {
        String url = container.getEmulatorHttpEndpoint();
        return BigQueryOptions
            .newBuilder()
            .setProjectId(container.getProjectId())
            .setHost(url)
            .setLocation(url)
            .setCredentials(NoCredentials.getInstance())
            .build().getService();
    }

    @Test
    void testHttpEndpoint() throws Exception {
        try (
            // emulatorContainer {
            BigQueryEmulatorContainer container = new BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.4.3")
            // }
        ) {
            container.start();

            // bigQueryClient {
            BigQuery bigQuery = getBigQuery(container);
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

    @Test
    void testGrcpEndpoint() throws IOException {
        try (BigQueryEmulatorContainer container = new BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.6.5")) {
            container.start();

            // Test setup.
            // Create a table the "regular" way. We need this to verify we can connect a writestream
            BigQuery bigQuery =  getBigQuery(container);
            String tableName = "test-table";
            String datasetName = "test-dataset";

            bigQuery.create(DatasetInfo.of(DatasetId.of(container.getProjectId(), datasetName)));

            Schema schema = Schema.of(
                Field.of("name", StandardSQLTypeName.STRING)
            );

            TableId tableId = TableId.of(datasetName, tableName);
            TableDefinition tableDefinition = StandardTableDefinition.of(schema);
            TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();

            bigQuery.create(tableInfo);

            // Actual test.
            // BigQueryWriteSettings requires a HTTP/2 connection, not provided by the originally exposed endpoint.
            BigQueryWriteSettings.Builder bigQueryWriteSettingsBuilder = BigQueryWriteSettings.newBuilder();

            bigQueryWriteSettingsBuilder.createWriteStreamSettings()
                .setRetrySettings(bigQueryWriteSettingsBuilder.createWriteStreamSettings()
                    .getRetrySettings()
                    .toBuilder()
                    .setTotalTimeout(Duration.ofSeconds(60))
                    .build());

            // Use the now exposed grpcPort to get a working connection.
            BigQueryWriteClient bigQueryWriteClient = BigQueryWriteClient.create(
                bigQueryWriteSettingsBuilder.setTransportChannelProvider(FixedTransportChannelProvider.create(GrpcTransportChannel.create(
                        ManagedChannelBuilder.forAddress(container.getHost(), container.getEmulatorGrpcPort()).usePlaintext().build())))
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .build()
            );

            TableName parentTable = TableName.of(container.getProjectId(), datasetName, tableName);
            CreateWriteStreamRequest createWriteStreamRequest = CreateWriteStreamRequest.newBuilder()
                .setParent(parentTable.toString())
                .setWriteStream(WriteStream.newBuilder().setType(WriteStream.Type.PENDING))
                .build();

            // Validate that we can successfully create a write stream. This would not work with http endpoint
            bigQueryWriteClient.createWriteStream(createWriteStreamRequest);

            bigQueryWriteClient.shutdown();
            bigQueryWriteClient.close();
        }
    }

}

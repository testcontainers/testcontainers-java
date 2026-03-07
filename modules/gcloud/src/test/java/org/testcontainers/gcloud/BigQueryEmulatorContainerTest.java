package org.testcontainers.gcloud;

import com.google.api.core.ApiFuture;
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
import com.google.cloud.bigquery.storage.v1.AppendRowsResponse;
import com.google.cloud.bigquery.storage.v1.BatchCommitWriteStreamsRequest;
import com.google.cloud.bigquery.storage.v1.BatchCommitWriteStreamsResponse;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteClient;
import com.google.cloud.bigquery.storage.v1.BigQueryWriteSettings;
import com.google.cloud.bigquery.storage.v1.CreateWriteStreamRequest;
import com.google.cloud.bigquery.storage.v1.FinalizeWriteStreamRequest;
import com.google.cloud.bigquery.storage.v1.FinalizeWriteStreamResponse;
import com.google.cloud.bigquery.storage.v1.JsonStreamWriter;
import com.google.cloud.bigquery.storage.v1.TableName;
import com.google.cloud.bigquery.storage.v1.WriteStream;
import io.grpc.ManagedChannelBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.threeten.bp.Duration;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BigQueryEmulatorContainerTest {

    @Test
    void testHttpEndpoint() throws Exception {
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

    @Test
    void testGrcpEndpoint() throws Exception {
        try (
            BigQueryEmulatorContainer container = new BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.6.5")
        ) {
            container.start();

            BigQuery bigQuery = getBigQuery(container);
            String tableName = "test-table";
            String datasetName = "test-dataset";

            bigQuery.create(DatasetInfo.of(DatasetId.of(container.getProjectId(), datasetName)));

            Schema schema = Schema.of(Field.of("name", StandardSQLTypeName.STRING));

            TableId tableId = TableId.of(datasetName, tableName);
            TableDefinition tableDefinition = StandardTableDefinition.of(schema);
            TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();

            bigQuery.create(tableInfo);

            BigQueryWriteSettings.Builder bigQueryWriteSettingsBuilder = BigQueryWriteSettings.newBuilder();

            bigQueryWriteSettingsBuilder
                .createWriteStreamSettings()
                .setRetrySettings(
                    bigQueryWriteSettingsBuilder
                        .createWriteStreamSettings()
                        .getRetrySettings()
                        .toBuilder()
                        .setTotalTimeout(Duration.ofSeconds(60))
                        .build()
                );

            BigQueryWriteClient bigQueryWriteClient = BigQueryWriteClient.create(
                bigQueryWriteSettingsBuilder
                    .setTransportChannelProvider(
                        FixedTransportChannelProvider.create(
                            GrpcTransportChannel.create(
                                ManagedChannelBuilder
                                    .forAddress(container.getHost(), container.getEmulatorGrpcPort())
                                    .usePlaintext()
                                    .build()
                            )
                        )
                    )
                    .setCredentialsProvider(NoCredentialsProvider.create())
                    .build()
            );

            TableName parentTable = TableName.of(container.getProjectId(), datasetName, tableName);
            CreateWriteStreamRequest createWriteStreamRequest = CreateWriteStreamRequest
                .newBuilder()
                .setParent(parentTable.toString())
                .setWriteStream(WriteStream.newBuilder().setType(WriteStream.Type.PENDING))
                .build();

            WriteStream writeStream = bigQueryWriteClient.createWriteStream(createWriteStreamRequest);

            JsonStreamWriter writer = JsonStreamWriter
                .newBuilder(writeStream.getName(), writeStream.getTableSchema(), bigQueryWriteClient)
                .build();

            JSONArray jsonArray = new JSONArray();
            JSONObject record1 = new JSONObject();
            record1.put("name", "Alice");
            jsonArray.put(record1);

            JSONObject record2 = new JSONObject();
            record2.put("name", "Bob");
            jsonArray.put(record2);

            ApiFuture<AppendRowsResponse> future = writer.append(jsonArray);
            AppendRowsResponse response = future.get();

            FinalizeWriteStreamRequest finalizeRequest = FinalizeWriteStreamRequest
                .newBuilder()
                .setName(writeStream.getName())
                .build();
            FinalizeWriteStreamResponse finalizeResponse = bigQueryWriteClient.finalizeWriteStream(finalizeRequest);

            BatchCommitWriteStreamsRequest commitRequest = BatchCommitWriteStreamsRequest
                .newBuilder()
                .setParent(parentTable.toString())
                .addWriteStreams(writeStream.getName())
                .build();
            BatchCommitWriteStreamsResponse commitResponse = bigQueryWriteClient.batchCommitWriteStreams(commitRequest);

            writer.close();

            String sql = String.format(
                "SELECT name FROM `%s.%s.%s` ORDER BY name",
                container.getProjectId(),
                datasetName,
                tableName
            );
            TableResult result = bigQuery.query(QueryJobConfiguration.newBuilder(sql).build());

            List<String> names = result
                .streamValues()
                .map(row -> row.get("name").getStringValue())
                .collect(Collectors.toList());

            assertThat(names).containsExactly("Alice", "Bob");

            bigQueryWriteClient.shutdown();
            bigQueryWriteClient.close();
        }
    }

    private BigQuery getBigQuery(BigQueryEmulatorContainer container) {
        String url = container.getEmulatorHttpEndpoint();
        return BigQueryOptions
            .newBuilder()
            .setProjectId(container.getProjectId())
            .setHost(url)
            .setLocation(url)
            .setCredentials(NoCredentials.getInstance())
            .build()
            .getService();
    }
}

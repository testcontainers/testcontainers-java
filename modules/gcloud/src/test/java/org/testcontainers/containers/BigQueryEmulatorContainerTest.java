package org.testcontainers.containers;

import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.DatasetInfo;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FormatOptions;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.JobStatus;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.TableDataWriteChannel;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.bigquery.WriteChannelConfiguration;
import org.awaitility.Awaitility;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.channels.Channels;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class BigQueryEmulatorContainerTest {

    @Test
    public void test() throws Exception {
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

    /**
     * When writing data to BigQuery, we run into the problem as documented
     * <a href="https://github.com/goccy/bigquery-emulator/issues/160#issuecomment-1801515384">here</a>.
     */
    @Test
    public void testThatTheContainerCanBeUsedWriteDataToBigQuery() throws Exception {
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

            bigQuery.create(DatasetInfo.of("write_test_dataset"));
            StandardTableDefinition tableDefinition = StandardTableDefinition.newBuilder()
                .setSchema(Schema.of(Field.of("value", StandardSQLTypeName.STRING)))
                .build();
            TableId tableId = TableId.of("write_test_dataset", "write_test");
            bigQuery.create(TableInfo.of(tableId, tableDefinition));

            WriteChannelConfiguration.Builder writeChannelConfiguration =
                WriteChannelConfiguration.newBuilder(tableId)
                    .setFormatOptions(FormatOptions.json())
                    .setWriteDisposition(JobInfo.WriteDisposition.WRITE_APPEND)
                    .setCreateDisposition(JobInfo.CreateDisposition.CREATE_NEVER)
                    .setSchema(tableDefinition.getSchema());
            TableDataWriteChannel writer = bigQuery.writer(writeChannelConfiguration.build());
            try (Writer sink = Channels.newWriter(writer, "UTF-8")) {
                sink.write("{\"value\":\"test\"}");
            }
            await()
                .atMost(Duration.ofSeconds(10))
                .until(() -> JobStatus.State.DONE.equals(writer.getJob().getStatus().getState()));

            String sql =
                "SELECT * FROM write_test_dataset.write_test";
            TableResult result = bigQuery.query(QueryJobConfiguration.newBuilder(sql).build());
            List<String> values = result
                .streamValues()
                .map(fieldValues -> fieldValues.get(0).getStringValue())
                .collect(Collectors.toList());
            assertThat(values).containsExactly("test");
        }
    }

    @Test
    public void testThatAnInitFunctionCanBeRunAfterTheContainerHasStarted() {
        try (
            // emulatorContainer {
            BigQueryEmulatorContainer container = new BigQueryEmulatorContainer("ghcr.io/goccy/bigquery-emulator:0.4.3")
            // }
        ) {
            Boolean[] initFunctionCalled = new Boolean[1];
            container.withInitFunction((ctr, reused) -> {
                initFunctionCalled[0] = true;
                assertThat(ctr).isSameAs(container);
                assertThat(reused).isFalse();
                assertThat(ctr.isRunning()).isTrue();
            });
            container.start();
            assertThat(initFunctionCalled[0]).isTrue();
        }
    }


}

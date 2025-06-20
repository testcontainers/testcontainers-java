package org.testcontainers.containers;

import com.google.cloud.NoCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

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
}

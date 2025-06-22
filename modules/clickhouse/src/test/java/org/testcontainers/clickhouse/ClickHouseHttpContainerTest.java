package org.testcontainers.clickhouse;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.Test;
import org.testcontainers.ClickhouseTestImages;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

public class ClickHouseHttpContainerTest {

    @Test
    public void testSimpleHttpQuery() throws IOException, ParseException {
        try (ClickHouseHttpContainer clickhouse = new ClickHouseHttpContainer(ClickhouseTestImages.CLICKHOUSE_IMAGE)) {
            clickhouse.start();

            String result = executeHttpQuery(clickhouse, "SELECT 1");
            assertThat(result.trim()).isEqualTo("1");
        }
    }

    @Test
    public void testCustomCredentials() throws IOException, ParseException {
        try (
            ClickHouseHttpContainer clickhouse = new ClickHouseHttpContainer(ClickhouseTestImages.CLICKHOUSE_IMAGE)
                .withUsername("custom_user")
                .withPassword("custom_password")
                .withDatabaseName("custom_db")
        ) {
            assertThat(clickhouse.getUsername()).isEqualTo("custom_user");
            assertThat(clickhouse.getPassword()).isEqualTo("custom_password");
            assertThat(clickhouse.getDatabaseName()).isEqualTo("custom_db");

            clickhouse.start();

            String result = executeHttpQuery(clickhouse, "SELECT 2");
            assertThat(result.trim()).isEqualTo("2");
        }
    }

    @Test
    public void testHttpUrlMethods() {
        try (ClickHouseHttpContainer clickhouse = new ClickHouseHttpContainer(ClickhouseTestImages.CLICKHOUSE_IMAGE)) {
            clickhouse.start();

            String httpUrl = clickhouse.getHttpUrl();
            assertThat(httpUrl).matches("http://localhost:\\d+");

            String httpUrlWithDb = clickhouse.getHttpUrl("test_db");
            assertThat(httpUrlWithDb).matches("http://localhost:\\d+/\\?database=test_db");

            String hostAddress = clickhouse.getHttpHostAddress();
            assertThat(hostAddress).matches("localhost:\\d+");

            Integer httpPort = clickhouse.getHttpPort();
            assertThat(httpPort).isGreaterThan(0);

            Integer nativePort = clickhouse.getNativePort();
            assertThat(nativePort).isGreaterThan(0);
            assertThat(nativePort).isNotEqualTo(httpPort);
        }
    }

    @Test
    public void testCreateTableAndInsert() throws IOException, ParseException {
        try (ClickHouseHttpContainer clickhouse = new ClickHouseHttpContainer(ClickhouseTestImages.CLICKHOUSE_IMAGE)) {
            clickhouse.start();

            // Create table
            executeHttpQuery(clickhouse, "CREATE TABLE test_table (id UInt32, name String) ENGINE = Memory");

            // Insert data
            executeHttpQuery(clickhouse, "INSERT INTO test_table VALUES (1, 'test')");

            // Query data
            String result = executeHttpQuery(clickhouse, "SELECT id, name FROM test_table");
            assertThat(result.trim()).isEqualTo("1\ttest");
        }
    }

    @Test
    public void testHealthCheck() throws IOException, ParseException {
        try (ClickHouseHttpContainer clickhouse = new ClickHouseHttpContainer(ClickhouseTestImages.CLICKHOUSE_IMAGE)) {
            clickhouse.start();

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet request = new HttpGet(clickhouse.getHttpUrl());

                try (CloseableHttpResponse response = client.execute(request)) {
                    assertThat(response.getCode()).isEqualTo(200);
                    String body = EntityUtils.toString(response.getEntity());
                    assertThat(body.trim()).isEqualTo("Ok.");
                }
            }
        }
    }

    @Test
    public void testNewVersionAuth() throws IOException, ParseException {
        try (
            ClickHouseHttpContainer clickhouse = new ClickHouseHttpContainer(
                ClickhouseTestImages.CLICKHOUSE_24_12_IMAGE
            )
        ) {
            clickhouse.start();

            String result = executeHttpQuery(clickhouse, "SELECT 1");
            assertThat(result.trim()).isEqualTo("1");
        }
    }

    private String executeHttpQuery(ClickHouseHttpContainer container, String query)
        throws IOException, ParseException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String url = container.getHttpUrl() + "/?database=" + container.getDatabaseName();
            HttpPost request = new HttpPost(url);

            String auth = container.getUsername() + ":" + container.getPassword();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            request.setHeader("Authorization", "Basic " + encodedAuth);

            StringEntity entity = new StringEntity(query, ContentType.TEXT_PLAIN);
            request.setEntity(entity);

            try (CloseableHttpResponse response = client.execute(request)) {
                if (response.getCode() != 200) {
                    String errorBody = EntityUtils.toString(response.getEntity());
                    throw new RuntimeException(
                        "HTTP request failed with status " + response.getCode() + ": " + errorBody
                    );
                }

                return EntityUtils.toString(response.getEntity());
            }
        }
    }
}

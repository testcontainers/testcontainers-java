package org.testcontainers.junit.questdb;

import io.questdb.client.Sender;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.containers.QuestDBContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

public final class SimpleQuestDBTest extends AbstractContainerDatabaseTest {
    private static final String TABLE_NAME = "mytable";

    @Test
    public void testConnectivity() throws SQLException {
        try (QuestDBContainer questdb = new QuestDBContainer(QuestDBTestImages.QUESTDB_TEST_IMAGE)) {
            questdb.start();

            ResultSet resultSet = performQuery(questdb, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testPgWire() {
        try (QuestDBContainer questdb = new QuestDBContainer(QuestDBTestImages.QUESTDB_TEST_IMAGE)) {
            questdb.start();
            populateByInfluxLineProtocol(questdb, 1_000);
            Awaitility.await().untilAsserted(() -> {
                try (ResultSet rs = performQuery(questdb, "SELECT count(*) from " + TABLE_NAME)) {
                    assertEquals(1_000, rs.getInt(1));
                }
            });
        }
    }

    @Test
    public void testRest() throws IOException {
        try (QuestDBContainer questdb = new QuestDBContainer(QuestDBTestImages.QUESTDB_TEST_IMAGE)) {
            questdb.start();
            populateByInfluxLineProtocol(questdb, 1_000);
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                String encodedSql = URLEncoder.encode("select * from " + TABLE_NAME, "UTF-8");
                HttpGet httpGet = new HttpGet(questdb.getHttpUrl() + "/exec?query=" + encodedSql);
                Awaitility.await().untilAsserted(() -> {
                    try (CloseableHttpResponse response = client.execute(httpGet)) {
                        Assert.assertEquals(200, response.getStatusLine().getStatusCode());
                        String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                        Assert.assertTrue("questdb response '" + json + "' does not contain expected count 1000", json.contains("\"count\":1000"));
                    }
                });
            }
        }
    }

    private static void populateByInfluxLineProtocol(QuestDBContainer questdb, int rowCount) {
        try (Sender sender = Sender.builder().address(questdb.getIlpUrl()).build()) {
            for (int i = 0; i < rowCount; i++) {
                sender.table(TABLE_NAME)
                    .symbol("sym", "sym1" + i)
                    .stringColumn("str", "str1" + i)
                    .longColumn("long", i)
                    .atNow();
            }
        }
    }
}

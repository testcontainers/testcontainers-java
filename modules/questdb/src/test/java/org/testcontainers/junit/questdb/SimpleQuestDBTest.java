package org.testcontainers.junit.questdb;

import io.questdb.client.Sender;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Test;
import org.testcontainers.QuestDBTestImages;
import org.testcontainers.containers.QuestDBContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class SimpleQuestDBTest extends AbstractContainerDatabaseTest {

    private static final String TABLE_NAME = "mytable";

    @Test
    public void testSimple() throws SQLException {
        try (QuestDBContainer questDB = new QuestDBContainer(QuestDBTestImages.QUESTDB_IMAGE)) {
            questDB.start();

            ResultSet resultSet = performQuery(questDB, questDB.getTestQueryString());

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }

    @Test
    public void testRest() throws IOException {
        try (QuestDBContainer questdb = new QuestDBContainer(QuestDBTestImages.QUESTDB_IMAGE)) {
            questdb.start();
            populateByInfluxLineProtocol(questdb, 1_000);
            try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                String encodedSql = URLEncoder.encode("select * from " + TABLE_NAME, "UTF-8");
                HttpGet httpGet = new HttpGet(questdb.getHttpUrl() + "/exec?query=" + encodedSql);
                await()
                    .untilAsserted(() -> {
                        try (CloseableHttpResponse response = client.execute(httpGet)) {
                            assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);
                            String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                            assertThat(json.contains("\"count\":1000")).isTrue();
                        }
                    });
            }
        }
    }

    private static void populateByInfluxLineProtocol(QuestDBContainer questdb, int rowCount) {
        try (Sender sender = Sender.builder().address(questdb.getIlpUrl()).build()) {
            for (int i = 0; i < rowCount; i++) {
                sender
                    .table(TABLE_NAME)
                    .symbol("sym", "sym1" + i)
                    .stringColumn("str", "str1" + i)
                    .longColumn("long", i)
                    .atNow();
            }
        }
    }
}

package com.example;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SolrQueryTest {

    private static final DockerImageName SOLR_IMAGE = DockerImageName.parse("solr:8.3.0");

    public static final SolrContainer solrContainer = new SolrContainer(SOLR_IMAGE)
        .withCollection(SolrSearchEngine.COLLECTION_NAME);

    private static SolrClient solrClient;

    @BeforeAll
    static void setUp() throws IOException, SolrServerException {
        solrContainer.start();
        solrClient =
            new Http2SolrClient.Builder(
                "http://" + solrContainer.getHost() + ":" + solrContainer.getSolrPort() + "/solr"
            )
                .build();

        // Add Sample Data
        solrClient.add(
            SolrSearchEngine.COLLECTION_NAME,
            Collections.singletonList(
                new SolrInputDocument(
                    createMap(
                        "id",
                        createInputField("id", "1"),
                        "title",
                        createInputField("title", "old skool - trainers - shoes")
                    )
                )
            )
        );

        solrClient.add(
            SolrSearchEngine.COLLECTION_NAME,
            Collections.singletonList(
                new SolrInputDocument(
                    createMap("id", createInputField("id", "2"), "title", createInputField("title", "print t-shirt"))
                )
            )
        );

        solrClient.commit(SolrSearchEngine.COLLECTION_NAME);
    }

    @Test
    void testQueryForShoes() {
        SolrSearchEngine searchEngine = new SolrSearchEngine(solrClient);

        SearchResult result = searchEngine.search("shoes");
        assertThat(result.getTotalHits()).as("When searching for shoes we expect one result").isEqualTo(1L);
        assertThat(result.getResults().get(0).get("id")).as("The result should have the id 1").isEqualTo("1");
    }

    @Test
    void testQueryForTShirt() {
        SolrSearchEngine searchEngine = new SolrSearchEngine(solrClient);

        SearchResult result = searchEngine.search("t-shirt");
        assertThat(result.getTotalHits()).as("When searching for t-shirt we expect one result").isEqualTo(1L);
        assertThat(result.getResults().get(0).get("id")).as("The result should have the id 2").isEqualTo("2");
    }

    @Test
    void testQueryForAsterisk() {
        SolrSearchEngine searchEngine = new SolrSearchEngine(solrClient);

        SearchResult result = searchEngine.search("*");
        assertThat(result.getTotalHits()).as("When searching for * we expect no results").isEqualTo(0L);
    }

    private static SolrInputField createInputField(String key, String value) {
        SolrInputField inputField = new SolrInputField(key);
        inputField.setValue(value);
        return inputField;
    }

    private static Map<String, SolrInputField> createMap(String k0, SolrInputField v0, String k1, SolrInputField v1) {
        Map<String, SolrInputField> result = new HashMap<>();
        result.put(k0, v0);
        result.put(k1, v1);
        return result;
    }
}

package com.example;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.SolrContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.example.SolrSearchEngine.COLLECTION_NAME;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class SolrQueryTest {

    private static final DockerImageName SOLR_IMAGE = DockerImageName.parse("solr:8.3.0");

    public static final SolrContainer solrContainer = new SolrContainer(SOLR_IMAGE)
        .withCollection(COLLECTION_NAME);

    private static SolrClient solrClient;

    @BeforeClass
    public static void setUp() throws IOException, SolrServerException {
        solrContainer.start();
        solrClient = new Http2SolrClient.Builder("http://" + solrContainer.getContainerIpAddress() + ":" + solrContainer.getSolrPort() + "/solr").build();

        // Add Sample Data
        solrClient.add(COLLECTION_NAME, Collections.singletonList(
            new SolrInputDocument(createMap(
                "id", createInputField("id", "1"),
                "title", createInputField("title", "old skool - trainers - shoes")
            ))
        ));

        solrClient.add(COLLECTION_NAME, Collections.singletonList(
            new SolrInputDocument(createMap(
                "id", createInputField("id", "2"),
                "title", createInputField("title", "print t-shirt")
            ))
        ));

        solrClient.commit(COLLECTION_NAME);
    }

    @Test
    public void testQueryForShoes() {
        SolrSearchEngine searchEngine = new SolrSearchEngine(solrClient);

        SearchResult result = searchEngine.search("shoes");
        assertEquals("When searching for shoes we expect one result", 1L, result.getTotalHits());
        assertEquals("The result should have the id 1", "1", result.getResults().get(0).get("id"));
    }

    @Test
    public void testQueryForTShirt() {
        SolrSearchEngine searchEngine = new SolrSearchEngine(solrClient);

        SearchResult result = searchEngine.search("t-shirt");
        assertEquals("When searching for t-shirt we expect one result", 1L, result.getTotalHits());
        assertEquals("The result should have the id 2", "2", result.getResults().get(0).get("id"));
    }

    @Test
    public void testQueryForAsterisk() {
        SolrSearchEngine searchEngine = new SolrSearchEngine(solrClient);

        SearchResult result = searchEngine.search("*");
        assertEquals("When searching for * we expect no results", 0L, result.getTotalHits());
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

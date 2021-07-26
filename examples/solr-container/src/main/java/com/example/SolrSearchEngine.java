package com.example;

import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.SolrDocument;

@RequiredArgsConstructor
public class SolrSearchEngine implements SearchEngine {

    public static final String COLLECTION_NAME = "products";

    private final SolrClient client;

    @SneakyThrows
    public SearchResult search(String term) {

        SolrQuery query = new SolrQuery();
        query.setQuery("title:" + ClientUtils.escapeQueryChars(term));
        QueryResponse response = client.query(COLLECTION_NAME, query);
        return createResult(response);
    }

    private SearchResult createResult(QueryResponse response) {
        return SearchResult.builder()
            .totalHits(response.getResults().getNumFound())
            .results(response.getResults()
                .stream()
                .map(SolrDocument::getFieldValueMap)
                .collect(Collectors.toList()))
            .build();
    }
}

package org.testcontainers.couchbase;

import com.couchbase.client.core.message.search.GetSearchIndexRequest;
import com.couchbase.client.core.message.search.GetSearchIndexResponse;
import com.couchbase.client.core.message.search.UpsertSearchIndexRequest;
import com.couchbase.client.core.message.search.UpsertSearchIndexResponse;
import org.junit.Assert;
import org.junit.Test;

public abstract class BaseCouchbaseContainerFtsTest extends BaseCouchbaseContainerTest {
    private static final String FTS_INDEX_NAME = "testIndex";
    private static final String FTS_INDEX =
        "{\n" +
            "  \"name\": \"name\",\n" +
            "  \"type\": \"fulltext-index\",\n" +
            "  \"params\": {\n" +
            "    \"mapping\": {\n" +
            "      \"default_mapping\": {\n" +
            "        \"enabled\": true,\n" +
            "        \"dynamic\": true\n" +
            "      },\n" +
            "      \"default_type\": \"_default\",\n" +
            "      \"default_analyzer\": \"standard\",\n" +
            "      \"default_datetime_parser\": \"dateTimeOptional\",\n" +
            "      \"default_field\": \"_all\",\n" +
            "      \"store_dynamic\": false,\n" +
            "      \"index_dynamic\": true\n" +
            "    },\n" +
            "    \"store\": {\n" +
            "      \"indexType\": \"scorch\",\n" +
            "      \"kvStoreName\": \"\"\n" +
            "    },\n" +
            "    \"doc_config\": {\n" +
            "      \"mode\": \"type_field\",\n" +
            "      \"type_field\": \"type\",\n" +
            "      \"docid_prefix_delim\": \"\",\n" +
            "      \"docid_regexp\": \"\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"sourceType\": \"couchbase\",\n" +
            "  \"sourceName\": \"" + TEST_BUCKET + "\"\n" +
            "}";

    @Test
    public void shouldCreateFtsIndex() {
        UpsertSearchIndexResponse upsertSearchIndexResponse = getBucket().core()
            .send(new UpsertSearchIndexRequest(FTS_INDEX_NAME, FTS_INDEX, TEST_BUCKET, DEFAULT_PASSWORD))
            .map(UpsertSearchIndexResponse.class::cast)
            .toBlocking()
            .single();
        Assert.assertTrue(upsertSearchIndexResponse.status().isSuccess());

        GetSearchIndexResponse getSearchIndexResponse = getBucket().core()
            .send(new GetSearchIndexRequest(FTS_INDEX_NAME, TEST_BUCKET, DEFAULT_PASSWORD))
            .map(GetSearchIndexResponse.class::cast)
            .toBlocking()
            .single();
        Assert.assertTrue(getSearchIndexResponse.status().isSuccess());
    }
}

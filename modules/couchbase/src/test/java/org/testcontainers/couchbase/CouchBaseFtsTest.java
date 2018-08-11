package org.testcontainers.couchbase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.cluster.DefaultBucketSettings;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.search.SearchQuery;
import com.couchbase.client.java.search.result.SearchQueryResult;

public class CouchBaseFtsTest extends AbstractCouchbaseTest {

    private static final String ID = "city";
    private static final String DOCUMENT_VALUE = "Rio de Janeiro"
        + " is a huge seaside city in Brazil, famed for its Copacabana"
        + " and Ipanema beaches, 38m Christ the Redeemer statue atop Mount"
        + " Corcovado and for Sugarloaf Mountain, a granite peak with cable"
        + "cars to its summit.";
    private static final String DOCUMENT = "{\"name\":\"" + DOCUMENT_VALUE + "\"}";
    private static CouchbaseContainer couchbaseContainer;

    @BeforeClass
    public static void initContainer() {
        couchbaseContainer = new CouchbaseContainer()
            .withNewBucket(DefaultBucketSettings.builder()
                .enableFlush(true)
                .name(TEST_BUCKET)
                .password(DEFAULT_PASSWORD)
                .quota(100)
                .replicas(0)
                .type(BucketType.COUCHBASE)
                .build())
            .withNewFts("fts_test");
        couchbaseContainer.start();
    }

    @Test
    public void shouldExecuteN1ql() throws Exception {
        getBucket().query(N1qlQuery.simple("INSERT INTO " + TEST_BUCKET + " (KEY, VALUE) VALUES ('" + ID + "', " + DOCUMENT + ")"));
        Thread.sleep(1000);
        SearchQueryResult result = getBucket().query(
            new SearchQuery("fts_test", SearchQuery.match("Corcovado")).fields("name"));
        Assert.assertEquals(1, result.hits().size());
        Assert.assertEquals(DOCUMENT_VALUE, getBucket().get(result.hits().get(0).id()).content().get("name"));
    }

    @Override
    protected CouchbaseContainer getCouchbaseContainer() {
        return couchbaseContainer;
    }
}

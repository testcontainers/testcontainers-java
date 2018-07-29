package org.testcontainers.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.cluster.DefaultBucketSettings;
import com.couchbase.client.java.document.RawJsonDocument;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import lombok.Getter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class Couchbase5_5Test {

    public static final String TEST_BUCKET = "test";
    public static final String DEFAULT_PASSWORD = "password";

    private static final String ID = "toto";

    private static final String DOCUMENT = "{\"name\":\"toto\"}";

    @Getter(lazy = true)
    private final static CouchbaseContainer couchbaseContainer = initCouchbaseContainer();

    @Getter(lazy = true)
    private final static Bucket bucket = openBucket(TEST_BUCKET, DEFAULT_PASSWORD);

    @After
    public void clear() {
        if (getCouchbaseContainer().isIndex() && getCouchbaseContainer().isQuery() && getCouchbaseContainer().isPrimaryIndex()) {
            getBucket().query(
                N1qlQuery.simple(String.format("DELETE FROM `%s`", getBucket().name()),
                    N1qlParams.build().consistency(ScanConsistency.STATEMENT_PLUS)));
        } else {
            getBucket().bucketManager().flush();
        }
    }

    private static CouchbaseContainer initCouchbaseContainer() {
        CouchbaseContainer couchbaseContainer = new CouchbaseContainer("couchbase/server:5.5.0")
            .withNewBucket(DefaultBucketSettings.builder()
                .enableFlush(true)
                .name(TEST_BUCKET)
                .password(DEFAULT_PASSWORD)
                .quota(100)
                .replicas(0)
                .type(BucketType.COUCHBASE)
                .build());
        couchbaseContainer.start();
        return couchbaseContainer;
    }

    private static Bucket openBucket(String bucketName, String password) {
        CouchbaseCluster cluster = getCouchbaseContainer().getCouchbaseCluster();
        Bucket bucket = cluster.openBucket(bucketName, password);
        Runtime.getRuntime().addShutdownHook(new Thread(bucket::close));
        return bucket;
    }

    @Test
    public void shouldInsertDocument() {
        RawJsonDocument expected = RawJsonDocument.create(ID, DOCUMENT);
        getBucket().upsert(expected);
        RawJsonDocument result = getBucket().get(ID, RawJsonDocument.class);
        Assert.assertEquals(expected.content(), result.content());
    }
}

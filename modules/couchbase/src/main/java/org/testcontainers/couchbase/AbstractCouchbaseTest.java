package org.testcontainers.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.cluster.DefaultBucketSettings;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import org.junit.After;

/**
 * Basic class that can be used for couchbase tests. It will clear the database after every test.
 */
public abstract class AbstractCouchbaseTest {

    public static final String TEST_BUCKET = "test";

    public static final String DEFAULT_PASSWORD = "password";

    /**
     * You can overwrite this in subclasses of this test to change the image.
     * This is necessary to be able to reuse the couchbaseContainer.
     */
    protected static String imageName = "couchbase/server:5.1.0";

    private static CouchbaseContainer couchbaseContainer;

    private static Bucket bucket;

    protected synchronized static void tearDownContext() {
        if (bucket != null) {
            bucket.close();
            bucket = null;
        }
        if (couchbaseContainer != null) {
            couchbaseContainer.getCouchbaseCluster().disconnect();
            couchbaseContainer.getCouchbaseEnvironment().shutdown();
            couchbaseContainer.stop();
            couchbaseContainer = null;
        }
    }

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

    protected static synchronized CouchbaseContainer getCouchbaseContainer() {
        if (couchbaseContainer == null) {
            couchbaseContainer = initCouchbaseContainer();
        }
        return couchbaseContainer;
    }

    private static CouchbaseContainer initCouchbaseContainer() {
        CouchbaseContainer couchbaseContainer = (imageName == null) ? new CouchbaseContainer() : new CouchbaseContainer(imageName);
        couchbaseContainer.withNewBucket(DefaultBucketSettings.builder()
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

    protected static synchronized Bucket getBucket() {
        if (bucket == null) {
            bucket = openBucket(TEST_BUCKET, DEFAULT_PASSWORD);
        }
        return bucket;
    }

    private static Bucket openBucket(String bucketName, String password) {
        CouchbaseCluster cluster = getCouchbaseContainer().getCouchbaseCluster();
        Bucket bucket = cluster.openBucket(bucketName, password);
        Runtime.getRuntime().addShutdownHook(new Thread(bucket::close));
        return bucket;
    }
}

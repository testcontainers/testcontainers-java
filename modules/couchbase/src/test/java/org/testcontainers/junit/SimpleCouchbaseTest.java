package org.testcontainers.junit;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.bucket.BucketType;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.cluster.DefaultBucketSettings;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.query.N1qlParams;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.N1qlQueryResult;
import com.couchbase.client.java.query.consistency.ScanConsistency;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.CouchbaseContainer;

import java.io.*;
import java.net.URLConnection;

import static org.rnorth.visibleassertions.VisibleAssertions.*;


/**
 * @author ldoguin
 */
public class SimpleCouchbaseTest {

    public static final String clusterUser = "Administrator";
    public static final String clusterPassword = "password";

    @Rule
    public CouchbaseContainer couchbase = new CouchbaseContainer()
            .withIndex(true)
            .withQuery(true)
            .withTravelSample(true)
            .withClusterUsername(clusterUser)
            .withClusterPassword(clusterPassword)
            .withNewBucket(DefaultBucketSettings.builder().enableFlush(true).name("default").quota(100).replicas(0).type(BucketType.COUCHBASE).build());


    @Test
    public void testSimple() throws Exception {
        CouchbaseCluster cluster = couchbase.getCouchbaseCluster();

        // Open default bucket
        Bucket defaultBucket = cluster.openBucket();
        Assert.assertNotNull(defaultBucket);
        // Open travel sample bucket
        Bucket travelSample = cluster.openBucket("travel-sample");
        Assert.assertNotNull(travelSample);

        // verify Credentials
        ClusterManager manager = cluster.clusterManager(clusterUser, clusterPassword);
        Assert.assertNotNull(manager);

        // Verify KV, Query and Index Service
        JsonObject object = JsonObject.create();
        object.put("key","value");
        object.put("is","awesome");
        JsonDocument doc = JsonDocument.create("testDoc");
        defaultBucket.insert(doc);
        N1qlParams params = N1qlParams.build().consistency(ScanConsistency.STATEMENT_PLUS);
        N1qlQuery q = N1qlQuery.simple("Select * from `default` limit 1", params);

        N1qlQueryResult result = travelSample.query(q);
        Assert.assertEquals(1, result.allRows().size());

    }

}

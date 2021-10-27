/*
 * Copyright (c) 2020 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.testcontainers.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.Collection;
import com.couchbase.client.java.json.JsonObject;
import org.junit.Test;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.function.Consumer;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

public class CouchbaseContainerTest {

    private static final DockerImageName COUCHBASE_IMAGE_ENTERPRISE =
        DockerImageName.parse("couchbase/server:enterprise-6.6.2");
    private static final DockerImageName COUCHBASE_IMAGE_COMMUNITY =
        DockerImageName.parse("couchbase/server:community-6.6.0");

    @Test
    public void testBasicContainerUsageForEnterpriseContainer() {
        // bucket_definition {
        BucketDefinition bucketDefinition = new BucketDefinition("mybucket");
        // }

        try (
            // container_definition {
            CouchbaseContainer container = new CouchbaseContainer(COUCHBASE_IMAGE_ENTERPRISE)
                .withBucket(bucketDefinition)
            // }
        ) {
            setUpClient(container, cluster -> {
                Bucket bucket = cluster.bucket(bucketDefinition.getName());
                bucket.waitUntilReady(Duration.ofSeconds(10L));

                Collection collection = bucket.defaultCollection();

                collection.upsert("foo", JsonObject.create().put("key", "value"));

                JsonObject fooObject = collection.get("foo").contentAsObject();

                assertEquals("value", fooObject.getString("key"));
            });
        }
    }

    @Test
    public void testBasicContainerUsageForCommunityContainer() {
        BucketDefinition bucketDefinition = new BucketDefinition("mybucket");

        try (
            CouchbaseContainer container = new CouchbaseContainer(COUCHBASE_IMAGE_COMMUNITY)
                .withBucket(bucketDefinition)
        ) {
            setUpClient(container, cluster -> {
                Bucket bucket = cluster.bucket(bucketDefinition.getName());
                bucket.waitUntilReady(Duration.ofSeconds(10L));

                Collection collection = bucket.defaultCollection();

                collection.upsert("foo", JsonObject.create().put("key", "value"));

                JsonObject fooObject = collection.get("foo").contentAsObject();

                assertEquals("value", fooObject.getString("key"));
            });
        }
    }

    @Test
    public void testBucketIsFlushableIfEnabled() {
        BucketDefinition bucketDefinition = new BucketDefinition("mybucket")
            .withFlushEnabled(true);

        try (
            CouchbaseContainer container = new CouchbaseContainer(COUCHBASE_IMAGE_ENTERPRISE)
                .withBucket(bucketDefinition)
        ) {
            setUpClient(container, cluster -> {
                Bucket bucket = cluster.bucket(bucketDefinition.getName());
                bucket.waitUntilReady(Duration.ofSeconds(10L));

                Collection collection = bucket.defaultCollection();

                collection.upsert("foo", JsonObject.create().put("key", "value"));

                cluster.buckets().flushBucket(bucketDefinition.getName());

                await().untilAsserted(() -> assertFalse(collection.exists("foo").exists()));
            });
        }
    }

    /**
     * Make sure that the code fails fast if the Analytics service is enabled on the community
     * edition which is not supported.
     */
    @Test
    public void testFailureIfCommunityUsedWithAnalytics() {
        try (
            CouchbaseContainer container = new CouchbaseContainer(COUCHBASE_IMAGE_COMMUNITY)
                .withEnabledServices(CouchbaseService.KV, CouchbaseService.ANALYTICS)
        ) {
            assertThrows(ContainerLaunchException.class, () -> setUpClient(container, cluster -> {}));
        }
    }

    private void setUpClient(CouchbaseContainer container, Consumer<Cluster> consumer) {
        container.start();

        // cluster_creation {
        Cluster cluster = Cluster.connect(
            container.getConnectionString(),
            container.getUsername(),
            container.getPassword()
        );
        // }

        try {
            consumer.accept(cluster);
        } finally {
            cluster.disconnect();
        }
    }
}

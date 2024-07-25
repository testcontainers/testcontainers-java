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

import java.time.Duration;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

public class CouchbaseContainerTest {

    private static final String COUCHBASE_IMAGE_ENTERPRISE = "couchbase/server:enterprise-7.0.3";

    private static final String COUCHBASE_IMAGE_ENTERPRISE_RECENT = "couchbase/server:enterprise-7.6.2";

    private static final String COUCHBASE_IMAGE_COMMUNITY = "couchbase/server:community-7.0.2";

    private static final String COUCHBASE_IMAGE_COMMUNITY_RECENT = "couchbase/server:community-7.6.2";

    @Test
    public void testBasicContainerUsageForEnterpriseContainer() {
        testBasicContainerUsage(COUCHBASE_IMAGE_ENTERPRISE);
    }

    @Test
    public void testBasicContainerUsageForEnterpriseContainerRecent() {
        testBasicContainerUsage(COUCHBASE_IMAGE_ENTERPRISE_RECENT);
    }

    @Test
    public void testBasicContainerUsageForCommunityContainer() {
        testBasicContainerUsage(COUCHBASE_IMAGE_COMMUNITY);
    }

    @Test
    public void testBasicContainerUsageForCommunityContainerRecent() {
        testBasicContainerUsage(COUCHBASE_IMAGE_COMMUNITY_RECENT);
    }

    private void testBasicContainerUsage(String couchbaseImage) {
        // bucket_definition {
        BucketDefinition bucketDefinition = new BucketDefinition("mybucket");
        // }

        try (
            // container_definition {
            CouchbaseContainer container = new CouchbaseContainer(couchbaseImage).withBucket(bucketDefinition)
            // }
        ) {
            setUpClient(
                container,
                cluster -> {
                    Bucket bucket = cluster.bucket(bucketDefinition.getName());
                    bucket.waitUntilReady(Duration.ofSeconds(10L));

                    Collection collection = bucket.defaultCollection();

                    collection.upsert("foo", JsonObject.create().put("key", "value"));

                    JsonObject fooObject = collection.get("foo").contentAsObject();

                    assertThat(fooObject.getString("key")).isEqualTo("value");
                }
            );
        }
    }

    @Test
    public void testBucketIsFlushableIfEnabled() {
        BucketDefinition bucketDefinition = new BucketDefinition("mybucket").withFlushEnabled(true);

        try (
            CouchbaseContainer container = new CouchbaseContainer(COUCHBASE_IMAGE_ENTERPRISE)
                .withBucket(bucketDefinition)
        ) {
            setUpClient(
                container,
                cluster -> {
                    Bucket bucket = cluster.bucket(bucketDefinition.getName());
                    bucket.waitUntilReady(Duration.ofSeconds(10L));

                    Collection collection = bucket.defaultCollection();

                    collection.upsert("foo", JsonObject.create().put("key", "value"));

                    cluster.buckets().flushBucket(bucketDefinition.getName());

                    await().untilAsserted(() -> assertThat(collection.exists("foo").exists()).isFalse());
                }
            );
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
            assertThatThrownBy(() -> {
                    setUpClient(container, cluster -> {});
                })
                .isInstanceOf(ContainerLaunchException.class);
        }
    }

    /**
     * Make sure that the code fails fast if the Eventing service is enabled on the community
     * edition which is not supported.
     */
    @Test
    public void testFailureIfCommunityUsedWithEventing() {
        try (
            CouchbaseContainer container = new CouchbaseContainer(COUCHBASE_IMAGE_COMMUNITY)
                .withEnabledServices(CouchbaseService.KV, CouchbaseService.EVENTING)
        ) {
            assertThatThrownBy(() -> {
                    setUpClient(container, cluster -> {});
                })
                .isInstanceOf(ContainerLaunchException.class);
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

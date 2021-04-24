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
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CouchbaseContainerTest {

    private static final DockerImageName COUCHBASE_IMAGE = DockerImageName.parse("couchbase/server:6.5.1");

    @Test
    public void testBasicContainerUsage() {
        // bucket_definition {
        BucketDefinition bucketDefinition = new BucketDefinition("mybucket");
        // }

        try (
            // container_definition {
            CouchbaseContainer container = new CouchbaseContainer(COUCHBASE_IMAGE)
                .withBucket(bucketDefinition)
            // }
        ) {
            container.start();

            // cluster_creation {
            CouchbaseEnvironment environment = DefaultCouchbaseEnvironment
                .builder()
                .bootstrapCarrierDirectPort(container.getBootstrapCarrierDirectPort())
                .bootstrapHttpDirectPort(container.getBootstrapHttpDirectPort())
                .build();

            Cluster cluster = CouchbaseCluster.create(
                environment,
                container.getHost()
            );
            // }

            try {
                // auth {
                cluster.authenticate(container.getUsername(), container.getPassword());
                // }

                Bucket bucket = cluster.openBucket(bucketDefinition.getName());

                bucket.upsert(JsonDocument.create("foo", JsonObject.empty()));

                assertTrue(bucket.exists("foo"));
                assertNotNull(cluster.clusterManager().getBucket(bucketDefinition.getName()));
            } finally {
                cluster.disconnect();
                environment.shutdown();
            }
        }
    }

}

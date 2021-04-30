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
import org.testcontainers.utility.DockerImageName;

import static org.junit.Assert.assertEquals;

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
            Cluster cluster = Cluster.connect(
                container.getConnectionString(),
                container.getUsername(),
                container.getPassword()
            );
            // }

            try {
                Bucket bucket = cluster.bucket(bucketDefinition.getName());
                Collection collection = bucket.defaultCollection();

                collection.upsert("foo", JsonObject.create().put("key", "value"));

                JsonObject fooObject = collection.get("foo").contentAsObject();

                assertEquals("value", fooObject.getString("key"));
            } finally {
                cluster.disconnect();
            }
        }
    }

}

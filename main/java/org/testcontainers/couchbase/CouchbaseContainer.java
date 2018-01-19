/*
 * Copyright (c) 2016 Couchbase, Inc.
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

import com.couchbase.client.core.utils.Base64;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.*;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.query.Index;
import lombok.Getter;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.HttpWaitStrategy;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Based on Laurent Doguin version
 *
 * Optimized by ctayeb
 */
public class CouchbaseContainer<SELF extends CouchbaseContainer<SELF>> extends GenericContainer<SELF> {

    private String memoryQuota = "300";

    private String indexMemoryQuota = "300";

    private String clusterUsername = "Administrator";

    private String clusterPassword = "password";

    private boolean keyValue = true;

    @Getter
    private boolean query = true;

    @Getter
    private boolean index = true;

    @Getter
    private boolean primaryIndex = true;

    @Getter
    private boolean fts = false;

    private boolean beerSample = false;

    private boolean travelSample = false;

    private boolean gamesIMSample = false;

    private CouchbaseEnvironment couchbaseEnvironment;

    private CouchbaseCluster couchbaseCluster;

    private List<BucketSettings> newBuckets = new ArrayList<>();

    private String urlBase;

    public CouchbaseContainer() {
        super("couchbase/server:latest");
    }

    public CouchbaseContainer(String containerName) {
        super(containerName);
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(8091);
    }

    @Override
    protected void configure() {
        // Configurable ports
        addExposedPorts(11210, 11207, 8091, 18091);

        // Non configurable ports
        addFixedExposedPort(8092, 8092);
        addFixedExposedPort(8093, 8093);
        addFixedExposedPort(8094, 8094);
        addFixedExposedPort(8095, 8095);
        addFixedExposedPort(18092, 18092);
        addFixedExposedPort(18093, 18093);
        setWaitStrategy(new HttpWaitStrategy().forPath("/ui/index.html#/"));
    }

    public CouchbaseEnvironment getCouchbaseEnvironment() {
        if (couchbaseEnvironment == null) {
            initCluster();
            couchbaseEnvironment = DefaultCouchbaseEnvironment.builder()
                    .bootstrapCarrierDirectPort(getMappedPort(11210))
                    .bootstrapCarrierSslPort(getMappedPort(11207))
                    .bootstrapHttpDirectPort(getMappedPort(8091))
                    .bootstrapHttpSslPort(getMappedPort(18091))
                    .build();
        }
        return couchbaseEnvironment;
    }

    public CouchbaseCluster getCouchbaseCluster() {
        if (couchbaseCluster == null) {
            couchbaseCluster = CouchbaseCluster.create(getCouchbaseEnvironment(), getContainerIpAddress());
        }
        return couchbaseCluster;
    }

    public SELF withClusterUsername(String username) {
        this.clusterUsername = username;
        return self();
    }

    public SELF withClusterPassword(String password) {
        this.clusterPassword = password;
        return self();
    }

    public SELF withMemoryQuota(String memoryQuota) {
        this.memoryQuota = memoryQuota;
        return self();
    }

    public SELF withIndexMemoryQuota(String indexMemoryQuota) {
        this.indexMemoryQuota = indexMemoryQuota;
        return self();
    }

    public SELF withKeyValue(boolean withKV) {
        this.keyValue = withKV;
        return self();
    }

    public SELF withIndex(boolean withIndex) {
        this.index = withIndex;
        return self();
    }

    public SELF withPrimaryIndex(boolean primaryIndex) {
        this.primaryIndex = primaryIndex;
        return self();
    }

    public SELF withQuery(boolean withQuery) {
        this.query = withQuery;
        return self();
    }

    public SELF withFTS(boolean withFTS) {
        this.fts = withFTS;
        return self();
    }

    public SELF withTravelSample() {
        this.travelSample = true;
        return self();
    }

    public SELF withBeerSample() {
        this.beerSample = true;
        return self();
    }

    public SELF withGamesIMSample() {
        this.gamesIMSample = true;
        return self();
    }

    public SELF withNewBucket(BucketSettings bucketSettings) {
        newBuckets.add(bucketSettings);
        return self();
    }


    public void initCluster() {
        urlBase = String.format("http://%s:%s", getContainerIpAddress(), getMappedPort(8091));
        try {
            String poolURL = "/pools/default";
            String poolPayload = "memoryQuota=" + URLEncoder.encode(memoryQuota, "UTF-8") + "&indexMemoryQuota=" + URLEncoder.encode(indexMemoryQuota, "UTF-8");

            String setupServicesURL = "/node/controller/setupServices";
            StringBuilder servicePayloadBuilder = new StringBuilder();
            if (keyValue) {
                servicePayloadBuilder.append("kv,");
            }
            if (query) {
                servicePayloadBuilder.append("n1ql,");
            }
            if (index) {
                servicePayloadBuilder.append("index,");
            }
            if (fts) {
                servicePayloadBuilder.append("fts,");
            }
            String setupServiceContent = "services=" + URLEncoder.encode(servicePayloadBuilder.toString(), "UTF-8");

            String webSettingsURL = "/settings/web";
            String webSettingsContent = "username=" + URLEncoder.encode(clusterUsername, "UTF-8") + "&password=" + URLEncoder.encode(clusterPassword, "UTF-8") + "&port=8091";

            String bucketURL = "/sampleBuckets/install";

            StringBuilder sampleBucketPayloadBuilder = new StringBuilder();
            sampleBucketPayloadBuilder.append('[');
            if (travelSample) {
                sampleBucketPayloadBuilder.append("\"travel-sample\",");
            }
            if (beerSample) {
                sampleBucketPayloadBuilder.append("\"beer-sample\",");
            }
            if (gamesIMSample) {
                sampleBucketPayloadBuilder.append("\"gamesim-sample\",");
            }
            sampleBucketPayloadBuilder.append(']');

            callCouchbaseRestAPI(poolURL, poolPayload);
            callCouchbaseRestAPI(setupServicesURL, setupServiceContent);
            callCouchbaseRestAPI(webSettingsURL, webSettingsContent);
            callCouchbaseRestAPI(bucketURL, sampleBucketPayloadBuilder.toString());

            CouchbaseWaitStrategy s = new CouchbaseWaitStrategy();
            s.withBasicCredentials(clusterUsername, clusterPassword);
            s.waitUntilReady(this);
            callCouchbaseRestAPI("/settings/indexes", "indexerThreads=0&logLevel=info&maxRollbackPoints=5&storageMode=memory_optimized");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void createBucket(BucketSettings bucketSetting, boolean primaryIndex) {
        ClusterManager clusterManager = getCouchbaseCluster().clusterManager(clusterUsername, clusterPassword);
        // Insert Bucket
        BucketSettings bucketSettings = clusterManager.insertBucket(bucketSetting);
        // Insert Bucket admin user
        UserSettings userSettings = UserSettings.build()
                .password(bucketSetting.password())
                .roles(Collections.singletonList(new UserRole("bucket_admin", bucketSetting.name())));
        clusterManager.upsertUser(AuthDomain.LOCAL, bucketSetting.name(), userSettings);
        if (index) {
            Bucket bucket = getCouchbaseCluster().openBucket(bucketSettings.name(), bucketSettings.password());
            new CouchbaseQueryServiceWaitStrategy(bucket).waitUntilReady(this);
            if (primaryIndex) {
                bucket.query(Index.createPrimaryIndex().on(bucketSetting.name()));
            }
        }
    }

    public void callCouchbaseRestAPI(String url, String payload) throws IOException {
        String fullUrl = urlBase + url;
        HttpURLConnection httpConnection = (HttpURLConnection) ((new URL(fullUrl).openConnection()));
        httpConnection.setDoOutput(true);
        httpConnection.setRequestMethod("POST");
        httpConnection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");
        String encoded = Base64.encode((clusterUsername + ":" + clusterPassword).getBytes("UTF-8"));
        httpConnection.setRequestProperty("Authorization", "Basic " + encoded);
        DataOutputStream out = new DataOutputStream(httpConnection.getOutputStream());
        out.writeBytes(payload);
        out.flush();
        out.close();
        httpConnection.getResponseCode();
        httpConnection.disconnect();
    }

    @Override
    public void start() {
        super.start();
        if (!newBuckets.isEmpty()) {
            for (BucketSettings bucketSetting : newBuckets) {
                createBucket(bucketSetting, primaryIndex);
            }
        }
    }

}

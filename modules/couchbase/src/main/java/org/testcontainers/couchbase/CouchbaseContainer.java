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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.google.common.collect.Lists;
import lombok.*;
import org.apache.commons.compress.utils.Sets;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.SocatContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.ThrowingFunction;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.testcontainers.couchbase.CouchbaseContainer.CouchbasePort.*;

/**
 * Based on Laurent Doguin version,
 * <p>
 * optimized by Tayeb Chlyah
 */
@AllArgsConstructor
public class CouchbaseContainer extends GenericContainer<CouchbaseContainer> {

    public static final String VERSION = "5.5.1";
    public static final String DOCKER_IMAGE_NAME = "couchbase/server:";
    public static final ObjectMapper MAPPER = new ObjectMapper();
    public static final String STATIC_CONFIG = "/opt/couchbase/etc/couchbase/static_config";
    public static final String CAPI_CONFIG = "/opt/couchbase/etc/couchdb/default.d/capi.ini";

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

    @Getter(lazy = true)
    private final CouchbaseEnvironment couchbaseEnvironment = createCouchbaseEnvironment();

    @Getter(lazy = true)
    private final CouchbaseCluster couchbaseCluster = createCouchbaseCluster();

    private List<BucketAndUserSettings> newBuckets = new ArrayList<>();

    private String urlBase;

    private SocatContainer proxy;

    public CouchbaseContainer() {
        this(DOCKER_IMAGE_NAME + VERSION);
    }

    public CouchbaseContainer(String containerName) {
        super(containerName);

        withNetwork(Network.SHARED);
        withNetworkAliases("couchbase-" + Base58.randomString(6));
        setWaitStrategy(new HttpWaitStrategy().forPath("/ui/index.html"));
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Sets.newHashSet(getMappedPort(REST));
    }

    @Override
    @SneakyThrows
    protected void doStart() {
        String networkAlias = getNetworkAliases().get(0);
        startProxy(networkAlias);

        for (CouchbasePort port : CouchbasePort.values()) {
            exposePortThroughProxy(networkAlias, port.getOriginalPort(), getMappedPort(port));
        }
        super.doStart();
    }

    private void startProxy(String networkAlias) {
        proxy = new SocatContainer().withNetwork(getNetwork());

        for (CouchbasePort port : CouchbasePort.values()) {
            if (port.isDynamic()) {
                proxy.withTarget(port.getOriginalPort(), networkAlias);
            } else {
                proxy.addExposedPort(port.getOriginalPort());
            }
        }
        proxy.setWaitStrategy(null);
        proxy.start();
    }

    private void exposePortThroughProxy(String networkAlias, int originalPort, int mappedPort) {
        ExecCreateCmdResponse createCmdResponse = dockerClient
            .execCreateCmd(proxy.getContainerId())
            .withCmd("/usr/bin/socat", "TCP-LISTEN:" + originalPort + ",fork,reuseaddr", "TCP:" + networkAlias + ":" + mappedPort)
            .exec();

        dockerClient.execStartCmd(createCmdResponse.getId())
            .exec(new ExecStartResultCallback());
    }

    @Override
    public List<Integer> getExposedPorts() {
        return proxy.getExposedPorts();
    }

    @Override
    public String getContainerIpAddress() {
        return proxy.getContainerIpAddress();
    }

    @Override
    public Integer getMappedPort(int originalPort) {
        return proxy.getMappedPort(originalPort);
    }

    protected Integer getMappedPort(CouchbasePort port) {
        return getMappedPort(port.getOriginalPort());
    }

    @Override
    public List<Integer> getBoundPortNumbers() {
        return proxy.getBoundPortNumbers();
    }

    @Override
    public void stop() {
        stopCluster();
        Stream.<Runnable>of(super::stop, proxy::stop).parallel().forEach(Runnable::run);
    }

    private void stopCluster() {
        getCouchbaseCluster().disconnect();
        getCouchbaseEnvironment().shutdown();
    }

    public CouchbaseContainer withNewBucket(BucketSettings bucketSettings) {
        newBuckets.add(new BucketAndUserSettings(bucketSettings));
        return self();
    }

    public CouchbaseContainer withNewBucket(BucketSettings bucketSettings, UserSettings userSettings) {
        newBuckets.add(new BucketAndUserSettings(bucketSettings, userSettings));
        return self();
    }

    @SneakyThrows
    public void initCluster() {
        urlBase = String.format("http://%s:%s", getContainerIpAddress(), getMappedPort(REST));
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

        callCouchbaseRestAPI(poolURL, poolPayload);
        callCouchbaseRestAPI(setupServicesURL, setupServiceContent);
        callCouchbaseRestAPI(webSettingsURL, webSettingsContent);

        createNodeWaitStrategy().waitUntilReady(this);
        callCouchbaseRestAPI("/settings/indexes", "indexerThreads=0&logLevel=info&maxRollbackPoints=5&storageMode=memory_optimized");
    }

    @NotNull
    private HttpWaitStrategy createNodeWaitStrategy() {
        return new HttpWaitStrategy()
            .forPath("/pools/default/")
            .withBasicCredentials(clusterUsername, clusterPassword)
            .forStatusCode(HTTP_OK)
            .forResponsePredicate(response -> {
                try {
                    return Optional.of(MAPPER.readTree(response))
                        .map(n -> n.at("/nodes/0/status"))
                        .map(JsonNode::asText)
                        .map("healthy"::equals)
                        .orElse(false);
                } catch (IOException e) {
                    logger().error("Unable to parse response {}", response);
                    return false;
                }
            });
    }

    public void createBucket(BucketSettings bucketSetting, UserSettings userSettings, boolean primaryIndex) {
        ClusterManager clusterManager = getCouchbaseCluster().clusterManager(clusterUsername, clusterPassword);
        // Insert Bucket
        BucketSettings bucketSettings = clusterManager.insertBucket(bucketSetting);
        try {
            // Insert Bucket user
            clusterManager.upsertUser(AuthDomain.LOCAL, bucketSetting.name(), userSettings);
        } catch (Exception e) {
            logger().warn("Unable to insert user '" + bucketSetting.name() + "', maybe you are using older version");
        }
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
        @Cleanup("disconnect")
        HttpURLConnection httpConnection = (HttpURLConnection) ((new URL(fullUrl).openConnection()));
        httpConnection.setDoOutput(true);
        httpConnection.setRequestMethod("POST");
        httpConnection.setRequestProperty("Content-Type",
            "application/x-www-form-urlencoded");
        String encoded = Base64.encode((clusterUsername + ":" + clusterPassword).getBytes("UTF-8"));
        httpConnection.setRequestProperty("Authorization", "Basic " + encoded);
        @Cleanup
        DataOutputStream out = new DataOutputStream(httpConnection.getOutputStream());
        out.writeBytes(payload);
        out.flush();
        httpConnection.getResponseCode();
    }

    @Override
    protected void containerIsCreated(String containerId) {
        patchConfig(STATIC_CONFIG, this::addMappedPorts);
        // capi needs a special configuration, see https://developer.couchbase.com/documentation/server/current/install/install-ports.html
        patchConfig(CAPI_CONFIG, this::replaceCapiPort);
    }

    private void patchConfig(String configLocation, ThrowingFunction<String, String> patchFunction) {
        String patchedConfig = copyFileFromContainer(configLocation,
            inputStream -> patchFunction.apply(IOUtils.toString(inputStream, StandardCharsets.UTF_8)));
        copyFileToContainer(Transferable.of(patchedConfig.getBytes(StandardCharsets.UTF_8)), configLocation);
    }

    private String addMappedPorts(String originalConfig) {
        String portConfig = Stream.of(CouchbasePort.values())
            .filter(port -> !port.isDynamic())
            .map(port -> String.format("{%s, %d}.", port.name, getMappedPort(port)))
            .collect(Collectors.joining("\n"));
        return String.format("%s\n%s", originalConfig, portConfig);
    }

    private String replaceCapiPort(String originalConfig) {
        return Arrays.stream(originalConfig.split("\n"))
            .map(s -> (s.matches("port\\s*=\\s*" + CAPI.getOriginalPort())) ? "port = " + getMappedPort(CAPI) : s)
            .collect(Collectors.joining("\n"));
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        if (!newBuckets.isEmpty()) {
            for (BucketAndUserSettings bucket : newBuckets) {
                createBucket(bucket.getBucketSettings(), bucket.getUserSettings(), primaryIndex);
            }
        }
    }

    private CouchbaseCluster createCouchbaseCluster() {
        return CouchbaseCluster.create(getCouchbaseEnvironment(), getContainerIpAddress());
    }

    private DefaultCouchbaseEnvironment createCouchbaseEnvironment() {
        initCluster();
        return DefaultCouchbaseEnvironment.builder()
            .kvTimeout(10000)
            .bootstrapCarrierDirectPort(getMappedPort(MEMCACHED))
            .bootstrapCarrierSslPort(getMappedPort(MEMCACHED_SSL))
            .bootstrapHttpDirectPort(getMappedPort(REST))
            .bootstrapHttpSslPort(getMappedPort(REST_SSL))
            .build();
    }

    public CouchbaseContainer withMemoryQuota(String memoryQuota) {
        this.memoryQuota = memoryQuota;
        return self();
    }

    public CouchbaseContainer withIndexMemoryQuota(String indexMemoryQuota) {
        this.indexMemoryQuota = indexMemoryQuota;
        return self();
    }

    public CouchbaseContainer withClusterAdmin(String username, String password) {
        this.clusterUsername = username;
        this.clusterPassword = password;
        return self();
    }

    public CouchbaseContainer withKeyValue(boolean keyValue) {
        this.keyValue = keyValue;
        return self();
    }

    public CouchbaseContainer withQuery(boolean query) {
        this.query = query;
        return self();
    }

    public CouchbaseContainer withIndex(boolean index) {
        this.index = index;
        return self();
    }

    public CouchbaseContainer withPrimaryIndex(boolean primaryIndex) {
        this.primaryIndex = primaryIndex;
        return self();
    }

    public CouchbaseContainer withFts(boolean fts) {
        this.fts = fts;
        return self();
    }

    @Getter
    @RequiredArgsConstructor
    protected enum CouchbasePort {
        REST("rest_port", 8091, true),
        CAPI("capi_port", 8092, false),
        QUERY("query_port", 8093, false),
        FTS("fts_http_port", 8094, false),
        CBAS("cbas_http_port", 8095, false),
        EVENTING("eventing_http_port", 8096, false),
        MEMCACHED_SSL("memcached_ssl_port", 11207, false),
        MEMCACHED("memcached_port", 11210, false),
        REST_SSL("ssl_rest_port", 18091, true),
        CAPI_SSL("ssl_capi_port", 18092, false),
        QUERY_SSL("ssl_query_port", 18093, false),
        FTS_SSL("fts_ssl_port", 18094, false),
        CBAS_SSL("cbas_ssl_port", 18095, false),
        EVENTING_SSL("eventing_ssl_port", 18096, false);

        final String name;

        final int originalPort;

        final boolean dynamic;
    }

    @Value
    @AllArgsConstructor
    private class BucketAndUserSettings {

        private final BucketSettings bucketSettings;
        private final UserSettings userSettings;

        public BucketAndUserSettings(final BucketSettings bucketSettings) {
            this.bucketSettings = bucketSettings;
            this.userSettings = UserSettings.build()
                .password(bucketSettings.password())
                .roles(getDefaultAdminRoles(bucketSettings.name()));
        }

        private List<UserRole> getDefaultAdminRoles(String bucketName) {
            return Lists.newArrayList(
                new UserRole("bucket_admin", bucketName),
                new UserRole("views_admin", bucketName),
                new UserRole("query_manage_index", bucketName),
                new UserRole("query_update", bucketName),
                new UserRole("query_select", bucketName),
                new UserRole("query_insert", bucketName),
                new UserRole("query_delete", bucketName)
            );
        }

    }
}

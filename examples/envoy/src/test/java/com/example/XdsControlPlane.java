package com.example;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.envoyproxy.controlplane.cache.v3.SimpleCache;
import io.envoyproxy.controlplane.cache.v3.Snapshot;
import io.envoyproxy.controlplane.server.V3DiscoveryServer;
import io.envoyproxy.envoy.config.cluster.v3.Cluster;
import io.envoyproxy.envoy.config.core.v3.Node;
import io.envoyproxy.envoy.config.listener.v3.Listener;
import io.envoyproxy.envoy.extensions.filters.http.rbac.v3.RBAC;
import io.envoyproxy.envoy.extensions.filters.http.router.v3.Router;
import io.envoyproxy.envoy.extensions.filters.network.http_connection_manager.v3.HttpConnectionManager;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A minimal xDS control plane based on envoyproxy/java-control-plane.
 * <p>
 * Each sidecar is identified by its Envoy node id and receives its own snapshot of listeners and clusters.
 * Calling {@link #setSnapshot(String, Listener, Cluster)} again pushes the new configuration to the running sidecar.
 */
class XdsControlPlane implements AutoCloseable {

    private static final JsonFormat.Parser JSON_PARSER = JsonFormat
        .parser()
        .usingTypeRegistry(
            JsonFormat.TypeRegistry
                .newBuilder()
                .add(HttpConnectionManager.getDescriptor())
                .add(Router.getDescriptor())
                .add(RBAC.getDescriptor())
                .build()
        );

    private final SimpleCache<String> cache = new SimpleCache<>(Node::getId);

    private final AtomicLong version = new AtomicLong();

    private final Server server;

    XdsControlPlane() {
        V3DiscoveryServer discoveryServer = new V3DiscoveryServer(cache);
        server = NettyServerBuilder.forPort(0).addService(discoveryServer.getAggregatedDiscoveryServiceImpl()).build();
        try {
            server.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    int getPort() {
        return server.getPort();
    }

    void setSnapshot(String nodeId, Listener listener, Cluster cluster) {
        cache.setSnapshot(
            nodeId,
            Snapshot.create(
                Collections.singletonList(cluster),
                Collections.emptyList(),
                Collections.singletonList(listener),
                Collections.emptyList(),
                Collections.emptyList(),
                String.valueOf(version.incrementAndGet())
            )
        );
    }

    static Listener listener(String resource, Map<String, String> variables) {
        Listener.Builder builder = Listener.newBuilder();
        mergeJson(render(resource, variables), builder);
        return builder.build();
    }

    static Cluster cluster(String resource) {
        Cluster.Builder builder = Cluster.newBuilder();
        mergeJson(render(resource, Collections.emptyMap()), builder);
        return builder.build();
    }

    /**
     * Reads a classpath resource and replaces every {@code ${name}} placeholder with its value.
     */
    static String render(String resource, Map<String, String> variables) {
        String content = readResource(resource);
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            content = content.replace("${" + variable.getKey() + "}", variable.getValue());
        }
        return content;
    }

    private static void mergeJson(String json, Message.Builder builder) {
        try {
            JSON_PARSER.merge(json, builder);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalArgumentException("Invalid xDS resource: " + json, e);
        }
    }

    private static String readResource(String resource) {
        try (InputStream in = XdsControlPlane.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("Resource not found: " + resource);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        server.shutdownNow();
    }
}

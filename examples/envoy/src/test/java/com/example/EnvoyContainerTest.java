package com.example;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.junit.jupiter.Container;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Shows how to set up a service mesh style environment with Testcontainers: every service gets an Envoy sidecar,
 * service-to-service traffic goes through the sidecars, and their configuration is delivered over xDS by a control
 * plane running in the test JVM (see {@link XdsControlPlane}).
 * <p>
 * The assertions only demonstrate that the example is wired correctly. They are not a test of Envoy itself.
 *
 * <pre>
 * test (client app) -> client-sidecar :15001 -> server-sidecar :15006 -> server-app :8080
 *                             ^                        ^
 *                             +------ xDS (ADS) -------+
 *                              control plane in test JVM
 * </pre>
 */
@org.testcontainers.junit.jupiter.Testcontainers
class EnvoyContainerTest {

    private static final String ENVOY_IMAGE = "envoyproxy/envoy:v1.39.1";

    private static final int OUTBOUND_PORT = 15001;

    private static final int INBOUND_PORT = 15006;

    private static final int ADMIN_PORT = 9901;

    // client-sidecar adds this value as the x-service-caller header to every outbound request
    private static final String CLIENT_CALLER = "client";

    private static final XdsControlPlane controlPlane = startControlPlane();

    private static final Network network = Network.newNetwork();

    @Container
    private static final GenericContainer<?> serverApp = new GenericContainer<>("mendhak/http-https-echo:41")
        .withNetwork(network)
        .withNetworkAliases("server-app")
        .withEnv("HTTP_PORT", "8080")
        .withExposedPorts(8080)
        .waitingFor(Wait.forHttp("/").forPort(8080));

    @Container
    private static final GenericContainer<?> serverSidecar = sidecar("server-sidecar", INBOUND_PORT)
        .dependsOn(serverApp);

    @Container
    private static final GenericContainer<?> clientSidecar = sidecar("client-sidecar", OUTBOUND_PORT)
        .dependsOn(serverSidecar);

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static XdsControlPlane startControlPlane() {
        XdsControlPlane controlPlane = new XdsControlPlane();
        // lets the sidecar containers reach the control plane through host.testcontainers.internal
        Testcontainers.exposeHostPorts(controlPlane.getPort());
        controlPlane.setSnapshot(
            "client-sidecar",
            XdsControlPlane.listener("xds/client-sidecar-listener.json", Collections.emptyMap()),
            XdsControlPlane.cluster("xds/client-sidecar-cluster.json")
        );
        allowCaller(controlPlane, CLIENT_CALLER);
        return controlPlane;
    }

    private static void allowCaller(XdsControlPlane controlPlane, String caller) {
        controlPlane.setSnapshot(
            "server-sidecar",
            XdsControlPlane.listener(
                "xds/server-sidecar-listener.json",
                Collections.singletonMap("ALLOWED_CALLER", caller)
            ),
            XdsControlPlane.cluster("xds/server-sidecar-cluster.json")
        );
    }

    private static GenericContainer<?> sidecar(String nodeId, int trafficPort) {
        String bootstrap = XdsControlPlane.render(
            "bootstrap.yaml",
            Collections.singletonMap("XDS_PORT", String.valueOf(controlPlane.getPort()))
        );
        return new GenericContainer<>(ENVOY_IMAGE)
            .withNetwork(network)
            .withNetworkAliases(nodeId)
            .withCopyToContainer(Transferable.of(bootstrap), "/etc/envoy/bootstrap.yaml")
            .withCommand("-c", "/etc/envoy/bootstrap.yaml", "--service-node", nodeId, "--service-cluster", nodeId)
            .withExposedPorts(trafficPort, ADMIN_PORT)
            // /ready returns 200 only after the listeners and clusters were received over xDS
            .waitingFor(Wait.forHttp("/ready").forPort(ADMIN_PORT));
    }

    @AfterAll
    static void stopControlPlane() {
        controlPlane.close();
    }

    @BeforeEach
    void resetCounters() throws Exception {
        send(
            HttpRequest.newBuilder(adminUri(clientSidecar, "/reset_counters")).POST(HttpRequest.BodyPublishers.noBody())
        );
    }

    @Test
    void sendsRequestThroughTheSidecars() throws Exception {
        HttpResponse<String> response = callServer("/hello");

        // the request reached server-app through client-sidecar and server-sidecar
        assertThat(response.statusCode()).isEqualTo(200);
        // client-sidecar marks outbound requests with the caller name, which server-sidecar authorizes
        // the echo application returns the received request as pretty-printed JSON
        String body = response.body().replaceAll("\\s", "");
        assertThat(body).contains("\"path\":\"/hello\"").contains("\"x-service-caller\":\"client\"");
    }

    /**
     * Configuration is not baked into the containers: the control plane can change it while the sidecars run,
     * which is how a mesh rolls out a new policy.
     */
    @Test
    void pushesConfigurationUpdateToARunningSidecar() throws Exception {
        allowCaller(controlPlane, "another-client");
        try {
            await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(callServer("/hello").statusCode()).isEqualTo(403));
        } finally {
            allowCaller(controlPlane, CLIENT_CALLER);
            await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(callServer("/hello").statusCode()).isEqualTo(200));
        }
    }

    @Test
    void configuresRetriesOnTheClientSidecar() throws Exception {
        // the echo application responds with the status code requested in this header, so the upstream service
        // can be made to fail on demand
        HttpResponse<String> response = callServer("/hello", "x-set-response-status-code", "503");

        assertThat(response.statusCode()).isEqualTo(503);
        // 1 original request + 2 retries configured in the client-sidecar route
        HttpResponse<String> stats = send(
            HttpRequest.newBuilder(adminUri(clientSidecar, "/stats?filter=cluster.server.upstream_rq_retry$"))
        );
        assertThat(stats.body()).contains("cluster.server.upstream_rq_retry: 2");
    }

    @Test
    void configuresTimeoutOnTheClientSidecar() throws Exception {
        // the echo application delays its response by this header value, longer than the 1s route timeout
        // configured for the client-sidecar
        HttpResponse<String> response = callServer("/hello", "x-set-response-delay-ms", "3000");

        assertThat(response.statusCode()).isEqualTo(504);
    }

    private HttpResponse<String> callServer(String path, String... headers) throws Exception {
        URI uri = URI.create(
            String.format("http://%s:%d%s", clientSidecar.getHost(), clientSidecar.getMappedPort(OUTBOUND_PORT), path)
        );
        HttpRequest.Builder request = HttpRequest.newBuilder(uri);
        if (headers.length > 0) {
            request.headers(headers);
        }
        return send(request);
    }

    private static URI adminUri(GenericContainer<?> sidecar, String path) {
        return URI.create(String.format("http://%s:%d%s", sidecar.getHost(), sidecar.getMappedPort(ADMIN_PORT), path));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}

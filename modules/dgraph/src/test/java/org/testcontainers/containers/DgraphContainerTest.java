package org.testcontainers.containers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.dgraph.DgraphClient;
import io.dgraph.DgraphGrpc;
import io.dgraph.DgraphProto;
import io.dgraph.Transaction;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.NonNull;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of functionality special to the DgraphContainer.
 *
 * @author Enrico Minack
 */
public class DgraphContainerTest {

    private static final String DGRAPH_VERSION_TAG = "v21.03.1";

    private static final DockerImageName DGRAPH_TEST_IMAGE = DockerImageName.parse("dgraph/dgraph:" + DGRAPH_VERSION_TAG);

    static private @NonNull DgraphClient getClient(@NonNull DgraphContainer container) {
        ManagedChannel channel = ManagedChannelBuilder
            .forTarget(container.getGrpcUrl())
            .usePlaintext().build();
        DgraphGrpc.DgraphStub stub = DgraphGrpc.newStub(channel);

        return new DgraphClient(stub);
    }

    private static void populateDgraph(@NonNull DgraphClient client) {
        Transaction tnx = client.newTransaction();
        tnx.mutate(
            DgraphProto.Mutation.newBuilder()
                .setSetNquads(ByteString.copyFromUtf8("_:a <label> \"a\" ."))
                .build()
        );
        tnx.commit();
    }

    private static void assertDgraphEquals(@NonNull DgraphClient client, @NonNull String json) {
        DgraphProto.Response response = client.newReadOnlyTransaction().doRequest(
            DgraphProto.Request.newBuilder().setQuery("query {\n" +
                "  result (func: has(label)) {\n" +
                "    <label>\n" +
                "  }\n" +
                "}").build()
        );
        assertThat(response.getJson().toStringUtf8()).isEqualTo("{\"result\":[" + json + "]}");
    }

    @Test
    public void shouldWorkWithZeroArguments() {
        try (
            DgraphContainer dgraphContainer = new DgraphContainer(DGRAPH_TEST_IMAGE)
        ) {
            assertThat(dgraphContainer.getZeroCommand()).isEqualTo(
                "dgraph zero"
            );
        }

        try (
            DgraphContainer dgraphContainer = new DgraphContainer(DGRAPH_TEST_IMAGE)
        ) {
            dgraphContainer
                .withZeroArgument("my", "host2:5080")
                .withZeroArgument("peer", "host1:5080")
                .withZeroArgumentValues("raft", "idx=2", "learner=false")
                .withZeroArgumentValues("telemetry", "reports=false")
                .withZeroArgumentValues("telemetry", "sentry=false");
            assertThat(dgraphContainer.getZeroCommand()).isEqualTo(
                "dgraph zero " +
                    "--my \"host2:5080\" " +
                    "--peer \"host1:5080\" " +
                    "--raft \"idx=2; learner=false\" " +
                    "--telemetry \"reports=false; sentry=false\""
            );
        }
    }

    @Test
    public void shouldWorkWithAlphaArguments() {
        try (
            DgraphContainer dgraphContainer = new DgraphContainer(DGRAPH_TEST_IMAGE)
        ) {
            assertThat(dgraphContainer.getAlphaCommand()).isEqualTo(
                "dgraph alpha " +
                    "--security \"whitelist=0.0.0.0/0\""
            );
        }

        try (
            DgraphContainer dgraphContainer = new DgraphContainer(DGRAPH_TEST_IMAGE)
        ) {
            dgraphContainer
                .withAlphaArgumentValues("security", "token=ABCDEFG");
            assertThat(dgraphContainer.getAlphaCommand()).isEqualTo(
                "dgraph alpha " +
                    "--security \"whitelist=0.0.0.0/0; token=ABCDEFG\""
            );
        }

        try (
            DgraphContainer dgraphContainer = new DgraphContainer(DGRAPH_TEST_IMAGE)
        ) {
            dgraphContainer
                .withAlphaArgument("security", "whitelist=10.0.0.0/8");
            assertThat(dgraphContainer.getAlphaCommand()).isEqualTo(
                "dgraph alpha " +
                    "--security \"whitelist=10.0.0.0/8\""
            );
        }

        try (
            DgraphContainer dgraphContainer = new DgraphContainer(DGRAPH_TEST_IMAGE)
        ) {
            dgraphContainer
                .withAlphaArgument("my", "host2:5080")
                .withAlphaArgument("peer", "host1:5080")
                .withAlphaArgumentValues("raft", "idx=2", "learner=false")
                .withAlphaArgumentValues("telemetry", "reports=false")
                .withAlphaArgumentValues("telemetry", "sentry=false");
            assertThat(dgraphContainer.getAlphaCommand()).isEqualTo(
                "dgraph alpha " +
                    "--my \"host2:5080\" " +
                    "--peer \"host1:5080\" " +
                    "--raft \"idx=2; learner=false\" " +
                    "--security \"whitelist=0.0.0.0/0\" " +
                    "--telemetry \"reports=false; sentry=false\""
            );
        }
    }

    @Test
    public void shouldReportRightVersion() {
        try (
            DgraphContainer dgraphContainer = new DgraphContainer(DGRAPH_TEST_IMAGE)
        ) {
            dgraphContainer.start();

            DgraphClient dgraphClient = getClient(dgraphContainer);

            assertThat(dgraphClient.checkVersion().getTag()).isEqualTo(DGRAPH_VERSION_TAG);
        }
    }

    @Test
    public void shouldAllowDropAll() {
        doTestAllowDropAll(DGRAPH_VERSION_TAG);
    }

    @Test
    public void shouldAllowDropAllPre2103() {
        doTestAllowDropAll("v20.11.0");
    }

    private void doTestAllowDropAll(@NonNull String imageVersion) {
        try (
            DgraphContainer dgraphContainer = new DgraphContainer(DockerImageName.parse("dgraph/dgraph:" + imageVersion))
        ) {
            dgraphContainer.start();

            DgraphClient dgraphClient = getClient(dgraphContainer);
            assertThat(dgraphClient.checkVersion().getTag()).isEqualTo(imageVersion);

            populateDgraph(dgraphClient);
            assertDgraphEquals(dgraphClient, "{\"label\":\"a\"}");

            dgraphClient.alter(DgraphProto.Operation.newBuilder().setDropAll(true).build());

            assertDgraphEquals(dgraphClient, "");
        }
    }

    @Test
    public void shouldAllowForCluster() {
        try (
            DgraphContainer dgraphContainerOne = new DgraphContainer(DGRAPH_TEST_IMAGE);
            DgraphContainer dgraphContainerTwo = new DgraphContainer(DGRAPH_TEST_IMAGE);
            DgraphContainer dgraphContainerThree = new DgraphContainer(DGRAPH_TEST_IMAGE)
        ) {
            Network network = Network.newNetwork();

            dgraphContainerOne
                .withZeroArgument("my", "dgraph-one:5080")
                .withZeroArgumentValues("raft", "idx=1")
                .withZeroArgument("replicas", "3")
                .withNetworkAliases("dgraph-one")
                .withNetwork(network)
                .start();

            dgraphContainerTwo
                .dependsOn(dgraphContainerOne)
                .withZeroArgument("my", "dgraph-two:5080")
                .withZeroArgument("peer", "dgraph-one:5080")
                .withZeroArgumentValues("raft", "idx=2")
                .withZeroArgument("replicas", "3")
                .withNetworkAliases("dgraph-two")
                .withNetwork(network)
                .start();

            dgraphContainerThree
                .dependsOn(dgraphContainerOne)
                .withZeroArgument("my", "dgraph-three:5080")
                .withZeroArgument("peer", "dgraph-one:5080")
                .withZeroArgumentValues("raft", "idx=3")
                .withZeroArgument("replicas", "3")
                .withNetworkAliases("dgraph-three")
                .withNetwork(network)
                .start();

            // fetch cluster state from first alpha
            try {
                URL url = new URL(dgraphContainerOne.getHttpUrl() + "/state");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                assertThat(con.getResponseCode()).isEqualTo(200);

                con.getInputStream();
                StringWriter writer = new StringWriter();
                IOUtils.copy(con.getInputStream(), writer, StandardCharsets.UTF_8);
                String json = writer.toString();

                ObjectMapper mapper = new ObjectMapper();
                TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
                Map<String, Object> map = mapper.readValue(json, typeRef);

                // check there are all three zeros in the cluster
                assertThat(map).containsKey("zeros");
                Map<String, Object> zeros = getMapValue(map, "zeros");
                assertThat(zeros).hasSize(3);
                assertThat(zeros).containsKey("1");
                assertThat(zeros).containsKey("2");
                assertThat(zeros).containsKey("3");

                Map<String, Object> zeroOne = getMapValue(zeros, "1");
                Map<String, Object> zeroTwo = getMapValue(zeros, "2");
                Map<String, Object> zeroThree = getMapValue(zeros, "3");

                // check the three zero node names
                assertThat(zeroOne).containsKey("addr");
                assertThat(zeroOne.get("addr")).isEqualTo("dgraph-one:5080");

                assertThat(zeroTwo).containsKey("addr");
                assertThat(zeroTwo.get("addr")).isEqualTo("dgraph-two:5080");

                assertThat(zeroThree).containsKey("addr");
                assertThat(zeroThree.get("addr")).isEqualTo("dgraph-three:5080");
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }

            // connect to the cluster (client will pick one of the alpha nodes)
            DgraphGrpc.DgraphStub[] stubs =
                Stream.of(dgraphContainerOne, dgraphContainerTwo, dgraphContainerThree)
                    .map(DgraphContainer::getGrpcUrl)
                    .map(ManagedChannelBuilder::forTarget)
                    .map(ManagedChannelBuilder::usePlaintext)
                    .map(ManagedChannelBuilder::build)
                    .map(DgraphGrpc::newStub)
                    .toArray(DgraphGrpc.DgraphStub[]::new);

            DgraphClient dgraphClient = new DgraphClient(stubs);

            assertThat(dgraphClient.checkVersion().getTag()).isEqualTo(DGRAPH_VERSION_TAG);
        }
    }

    private @NonNull Map<String, Object> getMapValue(@NonNull Map<String, Object> map, @NonNull String key) {
        if (map.containsKey(key)) {
            Object value = map.get(key);
            if (value instanceof Map<?, ?>) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapValue = (Map<String, Object>)value;
                return mapValue;
            }
        }
        throw new IllegalArgumentException();
    }
}

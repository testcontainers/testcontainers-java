package org.testcontainers.containers;

import com.google.protobuf.ByteString;
import io.dgraph.DgraphClient;
import io.dgraph.DgraphGrpc;
import io.dgraph.DgraphProto;
import io.dgraph.Transaction;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.NonNull;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of functionality special to the DgraphContainer.
 *
 * @author Enrico Minack
 */
public class DgraphContainerTest {

    private static final String DGRAPH_VERSION_TAG = "v21.03.1";

    private static final DockerImageName DGRAPH_TEST_IMAGE = DockerImageName.parse("dgraph/dgraph:" + DGRAPH_VERSION_TAG);

    static private @NonNull DgraphClient getClient(@NonNull DgraphContainer<?> container) {
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
    public void shouldReportRightVersion() {
        try (
            DgraphContainer<?> dgraphContainer = new DgraphContainer<>(DGRAPH_TEST_IMAGE)
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
            DgraphContainer<?> dgraphContainer = new DgraphContainer<>("dgraph/dgraph:" + imageVersion)
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
}

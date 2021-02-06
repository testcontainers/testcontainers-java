package org.testcontainers.containers;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.api.core.ApiFuture;
import com.google.api.gax.batching.Batcher;
import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.NoCredentialsProvider;
import com.google.api.gax.grpc.GrpcTransportChannel;
import com.google.api.gax.rpc.FixedTransportChannelProvider;
import com.google.api.gax.rpc.TransportChannelProvider;
import com.google.cloud.bigtable.admin.v2.BigtableInstanceAdminClient;
import com.google.cloud.bigtable.admin.v2.BigtableTableAdminClient;
import com.google.cloud.bigtable.admin.v2.models.CreateInstanceRequest;
import com.google.cloud.bigtable.admin.v2.models.CreateTableRequest;
import com.google.cloud.bigtable.admin.v2.models.Instance;
import com.google.cloud.bigtable.admin.v2.models.Table;
import com.google.cloud.bigtable.admin.v2.stub.BigtableInstanceAdminStub;
import com.google.cloud.bigtable.admin.v2.stub.BigtableInstanceAdminStubSettings;
import com.google.cloud.bigtable.admin.v2.stub.BigtableTableAdminStubSettings;
import com.google.cloud.bigtable.admin.v2.stub.EnhancedBigtableTableAdminStub;
import com.google.cloud.bigtable.data.v2.BigtableDataClient;
import com.google.cloud.bigtable.data.v2.BigtableDataSettings;
import com.google.cloud.bigtable.data.v2.models.Row;
import com.google.cloud.bigtable.data.v2.models.RowCell;
import com.google.cloud.bigtable.data.v2.models.RowMutation;
import com.google.cloud.bigtable.data.v2.models.RowMutationEntry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

public class BigtableEmulatorContainerTest {

    public static final String PROJECT_ID = "test-project";
    public static final String INSTANCE_ID = "test-instance";

    @Rule
    // emulatorContainer {
    public BigtableEmulatorContainer emulator = new BigtableEmulatorContainer(
        DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:316.0.0-emulators")
    );
    // }

    @Test
    // testWithEmulatorContainer {
    public void testSimple() throws IOException, InterruptedException, ExecutionException {
        ManagedChannel channel = ManagedChannelBuilder.forTarget(emulator.getEmulatorEndpoint())
            .usePlaintext().build();

        TransportChannelProvider channelProvider = FixedTransportChannelProvider.create(GrpcTransportChannel.create(channel));
        NoCredentialsProvider credentialsProvider = NoCredentialsProvider.create();

        try {
            createTable(channelProvider, credentialsProvider, "test-table" );

            BigtableDataClient client = BigtableDataClient.create(BigtableDataSettings
                .newBuilderForEmulator(emulator.getHost(), emulator.getEmulatorPort())
                .setProjectId(PROJECT_ID)
                .setInstanceId(INSTANCE_ID)
                .build());

            client.mutateRow(RowMutation.create("test-table", "1")
                .setCell("name", "firstName", "Ray"));

            Row row = client.readRow("test-table", "1");
            List<RowCell> cells = row.getCells("name", "firstName");

            assertThat(cells).isNotNull().hasSize(1);
            assertThat(cells.get(0).getValue().toStringUtf8()).isEqualTo("Ray");
        } finally {
            channel.shutdown();
        }
    }
    // }

    // createTable {
    private void createTable(TransportChannelProvider channelProvider, CredentialsProvider credentialsProvider, String tableName) throws IOException {
        EnhancedBigtableTableAdminStub stub = EnhancedBigtableTableAdminStub
            .createEnhanced(BigtableTableAdminStubSettings.newBuilder()
                .setTransportChannelProvider(channelProvider)
                .setCredentialsProvider(credentialsProvider)
                .build());

        try (BigtableTableAdminClient client = BigtableTableAdminClient.create(PROJECT_ID, INSTANCE_ID, stub)) {
            Table table = client.createTable(CreateTableRequest.of(tableName).addFamily("name"));
        }
    }
    // }
}

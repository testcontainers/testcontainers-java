package org.testcontainers.containers;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.NoCredentials;
import com.google.cloud.spanner.Database;
import com.google.cloud.spanner.DatabaseAdminClient;
import com.google.cloud.spanner.DatabaseClient;
import com.google.cloud.spanner.DatabaseId;
import com.google.cloud.spanner.Instance;
import com.google.cloud.spanner.InstanceAdminClient;
import com.google.cloud.spanner.InstanceConfigId;
import com.google.cloud.spanner.InstanceId;
import com.google.cloud.spanner.InstanceInfo;
import com.google.cloud.spanner.ResultSet;
import com.google.cloud.spanner.Spanner;
import com.google.cloud.spanner.SpannerOptions;
import com.google.cloud.spanner.Statement;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

public class SpannerEmulatorContainerTest {

    @Rule
    // emulatorContainer {
    public SpannerEmulatorContainer emulator = new SpannerEmulatorContainer(
        DockerImageName.parse("gcr.io/cloud-spanner-emulator/emulator:1.1.0")
    );
    // }

    private static final String PROJECT_NAME = "test-project";
    private static final String INSTANCE_NAME = "test-instance";
    private static final String DATABASE_NAME = "test-database";

    // testWithEmulatorContainer {
    @Test
    public void testSimple() throws ExecutionException, InterruptedException {
        SpannerOptions options = SpannerOptions.newBuilder()
                .setEmulatorHost(emulator.getEmulatorGrpcEndpoint())
                .setCredentials(NoCredentials.getInstance())
                .setProjectId(PROJECT_NAME)
                .build();

        Spanner spanner = options.getService();

        InstanceId instanceId = createInstance(spanner);

        createDatabase(spanner);

        DatabaseId databaseId = DatabaseId.of(instanceId, DATABASE_NAME);
        DatabaseClient dbClient = spanner.getDatabaseClient(databaseId);
        dbClient.readWriteTransaction()
                .run(tx -> {
                    String sql1 = "Delete from TestTable where 1=1";
                    tx.executeUpdate(Statement.of(sql1));
                    String sql = "INSERT INTO TestTable (Key, Value) VALUES (1, 'Java'), (2, 'Go')";
                    tx.executeUpdate(Statement.of(sql));
                    return null;
                });

        ResultSet resultSet = dbClient.readOnlyTransaction()
                .executeQuery(Statement.of("select * from TestTable order by Key"));
        resultSet.next();
        assertThat(resultSet.getLong(0)).isEqualTo(1);
        assertThat(resultSet.getString(1)).isEqualTo("Java");
    }
    // }

    // createDatabase {
    private void createDatabase(Spanner spanner) throws InterruptedException, ExecutionException {
        DatabaseAdminClient dbAdminClient = spanner.getDatabaseAdminClient();
        Database database = dbAdminClient.createDatabase(INSTANCE_NAME, DATABASE_NAME, Arrays.asList("CREATE TABLE TestTable (Key INT64, Value STRING(MAX)) PRIMARY KEY (Key)")).get();
    }
    // }

    // createInstance {
    private InstanceId createInstance(Spanner spanner) throws InterruptedException, ExecutionException {
        InstanceConfigId instanceConfig = InstanceConfigId.of(PROJECT_NAME, "emulator-config");
        InstanceId instanceId = InstanceId.of(PROJECT_NAME, INSTANCE_NAME);
        InstanceAdminClient insAdminClient = spanner.getInstanceAdminClient();
        Instance instance = insAdminClient.createInstance(InstanceInfo.newBuilder(instanceId).setNodeCount(1).setDisplayName("Test instance").setInstanceConfigId(instanceConfig).build()).get();
        return instanceId;
    }
    // }

}

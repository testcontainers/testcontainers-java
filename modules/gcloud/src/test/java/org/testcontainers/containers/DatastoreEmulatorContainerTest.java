package org.testcontainers.containers;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.cloud.NoCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

public class DatastoreEmulatorContainerTest {

    @Rule
    // creatingDatastoreEmulatorContainer {
    public DatastoreEmulatorContainer emulator = new DatastoreEmulatorContainer(
        DockerImageName.parse("gcr.io/google.com/cloudsdktool/cloud-sdk:316.0.0-emulators")
    );
    // }

    //  startingDatastoreEmulatorContainer {
    @Test
    public void testSimple() {
        DatastoreOptions options = DatastoreOptions.newBuilder()
                .setHost(emulator.getEmulatorEndpoint())
                .setCredentials(NoCredentials.getInstance())
                .setRetrySettings(ServiceOptions.getNoRetrySettings())
                .setProjectId("test-project")
                .build();
        Datastore datastore = options.getService();

        Key key = datastore.newKeyFactory().setKind("Task").newKey("sample");
        Entity entity = Entity.newBuilder(key).set("description", "my description").build();
        datastore.put(entity);

        assertThat(datastore.get(key).getString("description")).isEqualTo("my description");
    }
    // }

}

package org.testcontainers.containers;

import com.google.cloud.NoCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DatastoreEmulatorContainerTest {

    @Rule
    // creatingDatastoreEmulatorContainer {
    public DatastoreEmulatorContainer emulator = new DatastoreEmulatorContainer();
    // }

    //  startingDatastoreEmulatorContainer {
    @Test
    public void testSimple() {
        DatastoreOptions options = DatastoreOptions.newBuilder()
                .setHost(emulator.getContainerIpAddress() + ":" + emulator.getMappedPort(8081))
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

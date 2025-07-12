package org.testcontainers.containers;

import com.google.api.core.ApiFuture;
import com.google.cloud.NoCredentials;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteResult;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;

public class FirestoreEmulatorContainerTest {

    @Rule
    // emulatorContainer {
    public FirestoreEmulatorContainer emulator = new FirestoreEmulatorContainer(
        DockerImageName.parse("gcr.io/google.com/cloudsdktool/google-cloud-cli:441.0.0-emulators")
    );

    // }

    // testWithEmulatorContainer {
    @Test
    public void testSimple() throws ExecutionException, InterruptedException {
        FirestoreOptions options = FirestoreOptions
            .getDefaultInstance()
            .toBuilder()
            .setHost(emulator.getEmulatorEndpoint())
            .setCredentials(NoCredentials.getInstance())
            .setProjectId("test-project")
            .build();
        Firestore firestore = options.getService();

        CollectionReference users = firestore.collection("users");
        DocumentReference docRef = users.document("alovelace");
        Map<String, Object> data = new HashMap<>();
        data.put("first", "Ada");
        data.put("last", "Lovelace");
        ApiFuture<WriteResult> result = docRef.set(data);
        result.get();

        ApiFuture<QuerySnapshot> query = users.get();
        QuerySnapshot querySnapshot = query.get();

        assertThat(querySnapshot.getDocuments().get(0).getData()).containsEntry("first", "Ada");
    }

    // }

    @Test
    public void testWithFlags() {
        try (
            FirestoreEmulatorContainer emulator = new FirestoreEmulatorContainer(
                "gcr.io/google.com/cloudsdktool/google-cloud-cli:465.0.0-emulators"
            )
                .withFlags("--database-mode datastore-mode")
        ) {
            emulator.start();

            assertThat(emulator.getContainerInfo().getConfig().getCmd())
                .anyMatch(e -> e.contains("--database-mode datastore-mode"));
        }
    }
}

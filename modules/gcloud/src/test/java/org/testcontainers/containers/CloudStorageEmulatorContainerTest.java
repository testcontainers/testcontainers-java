package org.testcontainers.containers;

import com.google.cloud.NoCredentials;
import com.google.cloud.storage.*;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CloudStorageEmulatorContainerTest {

	// testWithCloudStorageEmulatorContainer {
	@Test
	public void shouldWriteAndReadFile() {
		final String BUCKET_NAME = "test-bucket";
		final String FILE_NAME = "test-file.txt";
		final String FILE_CONTENT = "test file content";

		try (
				CloudStorageEmulatorContainer container = new CloudStorageEmulatorContainer("fsouza/fake-gcs-server:1.50.2")
		) {
			container.start();

			Storage storageClient = StorageOptions.newBuilder()
					.setHost(container.getEmulatorHttpEndpoint())
					.setProjectId("test-project")
					.setCredentials(NoCredentials.getInstance())
					.build()
					.getService();
			storageClient.create(BucketInfo.of(BUCKET_NAME));

			storageClient.create(
					BlobInfo.newBuilder(BUCKET_NAME, FILE_NAME).build(),
					FILE_CONTENT.getBytes()
			);

			Blob blob = storageClient.get(BUCKET_NAME, FILE_NAME);
			assertThat(blob.getContent())
					.asString()
					.isEqualTo(FILE_CONTENT);
		}
	}
	// }

}

package org.testcontainers.containers;

import com.azure.core.credential.AzureNamedKeyCredential;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.junit.After;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class AzuriteContainerTest {

    private static final DockerImageName AZURITE_IMAGE = DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.27.0");

    @Test
    public void azuriteStartupTest() {
        try (AzuriteContainer container = new AzuriteContainer(AZURITE_IMAGE)) {
            container.withService(AzuriteService.BLOB);
            container.start();

            // Create Client
            // createBlobStorageClient {
            BlobServiceClient client = new BlobServiceClientBuilder()
                .endpoint(container.getEndpoint(AzuriteService.BLOB))
                .credential(new AzureNamedKeyCredential(container.getAccountName(), container.getAccountKey()))
                .buildClient();
            // }

            // Get Service Version
            assertThat(client.getServiceVersion().getVersion()).isNotEmpty();
        }
    }

    @Test
    @SuppressWarnings("resource")
    public void solrCloudPingTest() {
        // azuriteContainerUsage {
        // Create the solr container.
        AzuriteContainer container = new AzuriteContainer(AZURITE_IMAGE);

        // Activate the needed services
        container.withService(AzuriteService.BLOB, AzuriteService.QUEUE);

        // Start the container. This step might take some time...
        container.start();

        // Stop the container.
        container.stop();
        // }
    }

    public BlobServiceClient client(AzuriteContainer container) {
        return new BlobServiceClientBuilder()
            .endpoint(container.getEndpoint(AzuriteService.BLOB))
            .credential(new AzureNamedKeyCredential(container.getAccountName(), container.getAccountKey()))
            .buildClient();
    }
}

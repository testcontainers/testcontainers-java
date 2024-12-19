package org.testcontainers.containers;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueServiceClient;
import com.azure.storage.queue.QueueServiceClientBuilder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

public class AzuriteContainerTest {

    @Rule
    // blobEmulatorContainer {
    public AzuriteContainer blobEmulator = new AzuriteContainer(
        DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite")
    )
        .withBlobPort(10000)
        .withoutQueue()
        .withoutTable();

    // }

    @Rule
    // queueEmulatorContainer {
    public AzuriteContainer queueEmulator = new AzuriteContainer(
        DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite")
    )
        .withQueuePort(10001)
        .withoutBlob()
        .withoutTable();

    // }

    @Rule
    // tableEmulatorContainer {
    public AzuriteContainer tableEmulator = new AzuriteContainer(
        DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite")
    )
        .withTablePort(10002)
        .withoutBlob()
        .withoutQueue();

    // }

    @Test
    public void testWithBlobServiceClient() {
        // getBlobConnectionString {
        final String connectionString = blobEmulator.getDefaultConnectionString();
        // }
        // createBlobClient {
        final BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();
        // }
        // testWithBlobClient {
        final BlobContainerClient containerClient = blobServiceClient.createBlobContainer("test-container");

        Assert.assertTrue(containerClient.exists());
        // }
    }

    @Test
    public void testWithQueueServiceClient() {
        // getQueueConnectionString {
        final String connectionString = blobEmulator.getDefaultConnectionString();
        // }
        // createQueueClient {
        final QueueServiceClient queueServiceClient = new QueueServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();
        // }
        // testWithQueueClient {
        final QueueClient queueClient = queueServiceClient.createQueue("test-queue");

        Assert.assertNotNull(queueClient.getQueueUrl());
        // }
    }

    @Test
    public void testWithTableServiceClient() {
        // getTableConnectionString {
        final String connectionString = blobEmulator.getDefaultConnectionString();
        // }
        // createTableClient {
        final TableServiceClient tableServiceClient = new TableServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();
        // }
        // testWithTableClient {
        final TableClient tableClient = tableServiceClient.createTable("testtable");

        Assert.assertNotNull(tableClient.getTableEndpoint());
        // }
    }
}

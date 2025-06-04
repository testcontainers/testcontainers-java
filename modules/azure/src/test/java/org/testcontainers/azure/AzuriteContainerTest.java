package org.testcontainers.azure;

import com.azure.core.util.BinaryData;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueServiceClient;
import com.azure.storage.queue.QueueServiceClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.MountableFile;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class AzuriteContainerTest {

    private static final String PASSWORD = "changeit";

    private static Properties originalSystemProperties;

    @BeforeAll
    public static void captureOriginalSystemProperties() {
        originalSystemProperties = (Properties) System.getProperties().clone();
        System.setProperty(
            "javax.net.ssl.trustStore",
            MountableFile.forClasspathResource("/keystore.pfx").getFilesystemPath()
        );
        System.setProperty("javax.net.ssl.trustStorePassword", PASSWORD);
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
    }

    @AfterAll
    public static void restoreOriginalSystemProperties() {
        System.setProperties(originalSystemProperties);
    }

    @Test
    public void testWithBlobServiceClient() {
        try (
            // emulatorContainer {
            AzuriteContainer emulator = new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.33.0")
            // }
        ) {
            emulator.start();
            assertThat(emulator.getConnectionString()).contains("BlobEndpoint=http://");
            testBlob(emulator);
        }
    }

    @Test
    public void testWithQueueServiceClient() {
        try (AzuriteContainer emulator = new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.33.0")) {
            emulator.start();
            assertThat(emulator.getConnectionString()).contains("QueueEndpoint=http://");
            testQueue(emulator);
        }
    }

    @Test
    public void testWithTableServiceClient() {
        try (AzuriteContainer emulator = new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.33.0")) {
            emulator.start();
            assertThat(emulator.getConnectionString()).contains("TableEndpoint=http://");
            testTable(emulator);
        }
    }

    @Test
    public void testWithBlobServiceClientWithSslUsingPfx() {
        try (
            AzuriteContainer emulator = new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.33.0")
                .withSsl(MountableFile.forClasspathResource("/keystore.pfx"), PASSWORD)
        ) {
            emulator.start();
            assertThat(emulator.getConnectionString()).contains("BlobEndpoint=https://");
            testBlob(emulator);
        }
    }

    @Test
    public void testWithQueueServiceClientWithSslUsingPfx() {
        try (
            AzuriteContainer emulator = new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.33.0")
                .withSsl(MountableFile.forClasspathResource("/keystore.pfx"), PASSWORD)
        ) {
            emulator.start();
            assertThat(emulator.getConnectionString()).contains("QueueEndpoint=https://");
            testQueue(emulator);
        }
    }

    @Test
    public void testWithTableServiceClientWithSslUsingPfx() {
        try (
            AzuriteContainer emulator = new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.33.0")
                .withSsl(MountableFile.forClasspathResource("/keystore.pfx"), PASSWORD)
        ) {
            emulator.start();
            assertThat(emulator.getConnectionString()).contains("TableEndpoint=https://");
            testTable(emulator);
        }
    }

    @Test
    public void testWithBlobServiceClientWithSslUsingPem() {
        try (
            AzuriteContainer emulator = new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.33.0")
                .withSsl(
                    MountableFile.forClasspathResource("/certificate.pem"),
                    MountableFile.forClasspathResource("/key.pem")
                )
        ) {
            emulator.start();
            assertThat(emulator.getConnectionString()).contains("BlobEndpoint=https://");
            testBlob(emulator);
        }
    }

    @Test
    public void testWithQueueServiceClientWithSslUsingPem() {
        try (
            AzuriteContainer emulator = new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.33.0")
                .withSsl(
                    MountableFile.forClasspathResource("/certificate.pem"),
                    MountableFile.forClasspathResource("/key.pem")
                )
        ) {
            emulator.start();
            assertThat(emulator.getConnectionString()).contains("QueueEndpoint=https://");
            testQueue(emulator);
        }
    }

    @Test
    public void testWithTableServiceClientWithSslUsingPem() {
        try (
            AzuriteContainer emulator = new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.33.0")
                .withSsl(
                    MountableFile.forClasspathResource("/certificate.pem"),
                    MountableFile.forClasspathResource("/key.pem")
                )
        ) {
            emulator.start();
            assertThat(emulator.getConnectionString()).contains("TableEndpoint=https://");
            testTable(emulator);
        }
    }

    @Test
    public void testTwoAccountKeysWithBlobServiceClient() {
        try (
            // withTwoAccountKeys {
            AzuriteContainer emulator = new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.33.0")
                .withEnv("AZURITE_ACCOUNTS", "account1:key1:key2")
            // }
        ) {
            emulator.start();

            String connectionString1 = emulator.getConnectionString("account1", "key1");
            // the second account will have access to the same container using a different key
            String connectionString2 = emulator.getConnectionString("account1", "key2");

            BlobServiceClient blobServiceClient1 = new BlobServiceClientBuilder()
                .connectionString(connectionString1)
                .buildClient();

            BlobContainerClient containerClient1 = blobServiceClient1.createBlobContainer("test-container");
            BlobClient blobClient1 = containerClient1.getBlobClient("test-blob.txt");
            blobClient1.upload(BinaryData.fromString("content"));
            boolean existsWithAccount1 = blobClient1.exists();
            String contentWithAccount1 = blobClient1.downloadContent().toString();

            BlobServiceClient blobServiceClient2 = new BlobServiceClientBuilder()
                .connectionString(connectionString2)
                .buildClient();
            BlobContainerClient containerClient2 = blobServiceClient2.getBlobContainerClient("test-container");
            BlobClient blobClient2 = containerClient2.getBlobClient("test-blob.txt");
            boolean existsWithAccount2 = blobClient2.exists();
            String contentWithAccount2 = blobClient2.downloadContent().toString();

            assertThat(existsWithAccount1).isTrue();
            assertThat(contentWithAccount1).isEqualTo("content");
            assertThat(existsWithAccount2).isTrue();
            assertThat(contentWithAccount2).isEqualTo("content");
        }
    }

    @Test
    public void testMultipleAccountsWithBlobServiceClient() {
        try (
            // withMoreAccounts {
            AzuriteContainer emulator = new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.33.0")
                .withEnv("AZURITE_ACCOUNTS", "account1:key1;account2:key2")
            // }
        ) {
            emulator.start();

            // useNonDefaultCredentials {
            String connectionString1 = emulator.getConnectionString("account1", "key1");
            // the second account will not have access to the same container
            String connectionString2 = emulator.getConnectionString("account2", "key2");
            // }
            BlobServiceClient blobServiceClient1 = new BlobServiceClientBuilder()
                .connectionString(connectionString1)
                .buildClient();

            BlobContainerClient containerClient1 = blobServiceClient1.createBlobContainer("test-container");
            BlobClient blobClient1 = containerClient1.getBlobClient("test-blob.txt");
            blobClient1.upload(BinaryData.fromString("content"));
            boolean existsWithAccount1 = blobClient1.exists();
            String contentWithAccount1 = blobClient1.downloadContent().toString();

            BlobServiceClient blobServiceClient2 = new BlobServiceClientBuilder()
                .connectionString(connectionString2)
                .buildClient();
            BlobContainerClient containerClient2 = blobServiceClient2.createBlobContainer("test-container");
            BlobClient blobClient2 = containerClient2.getBlobClient("test-blob.txt");
            boolean existsWithAccount2 = blobClient2.exists();

            assertThat(existsWithAccount1).isTrue();
            assertThat(contentWithAccount1).isEqualTo("content");
            assertThat(existsWithAccount2).isFalse();
        }
    }

    private void testBlob(AzuriteContainer container) {
        // createBlobClient {
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(container.getConnectionString())
            .buildClient();
        // }
        BlobContainerClient containerClient = blobServiceClient.createBlobContainer("test-container");

        assertThat(containerClient.exists()).isTrue();
    }

    private void testQueue(AzuriteContainer container) {
        // createQueueClient {
        QueueServiceClient queueServiceClient = new QueueServiceClientBuilder()
            .connectionString(container.getConnectionString())
            .buildClient();
        // }
        QueueClient queueClient = queueServiceClient.createQueue("test-queue");

        assertThat(queueClient.getQueueUrl()).isNotNull();
    }

    private void testTable(AzuriteContainer container) {
        // createTableClient {
        TableServiceClient tableServiceClient = new TableServiceClientBuilder()
            .connectionString(container.getConnectionString())
            .buildClient();
        // }
        TableClient tableClient = tableServiceClient.createTable("testtable");

        assertThat(tableClient.getTableEndpoint()).isNotNull();
    }
}

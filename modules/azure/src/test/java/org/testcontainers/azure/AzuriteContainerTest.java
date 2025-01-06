package org.testcontainers.azure;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableServiceClient;
import com.azure.data.tables.TableServiceClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueServiceClient;
import com.azure.storage.queue.QueueServiceClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class AzuriteContainerTest {

    private static final String PASSWORD = "changeit";

    private static final String LOOPBACK_IP = "127.0.0.1";

    private static Properties originalSystemProperties;

    @BeforeClass
    public static void captureOriginalSystemProperties() {
        originalSystemProperties = (Properties) System.getProperties().clone();
        System.setProperty(
            "javax.net.ssl.trustStore",
            MountableFile.forClasspathResource("/keystore.pfx").getFilesystemPath()
        );
        System.setProperty("javax.net.ssl.trustStorePassword", PASSWORD);
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
    }

    @AfterClass
    public static void restoreOriginalSystemProperties() {
        System.setProperties(originalSystemProperties);
    }

    @Test
    public void testWithBlobServiceClient() {
        try (
            // emulatorContainer {
            AzuriteContainer emulator = new AzuriteContainer(
                DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.33.0")
            )
            // }
        ) {
            emulator.start();
            testBlob(emulator);
        }
    }

    @Test
    public void testWithQueueServiceClient() {
        try (
            AzuriteContainer emulator = new AzuriteContainer(
                DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.33.0")
            )
        ) {
            emulator.start();
            testQueue(emulator);
        }
    }

    @Test
    public void testWithTableServiceClient() {
        try (
            AzuriteContainer emulator = new AzuriteContainer(
                DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.33.0")
            )
        ) {
            emulator.start();
            testTable(emulator);
        }
    }

    @Test
    public void testWithBlobServiceClientWithSslUsingPfx() {
        try (
            AzuriteContainer emulator = new AzuriteContainer(
                DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.33.0")
            )
                .withSsl(MountableFile.forClasspathResource("/keystore.pfx"), PASSWORD)
                .withHost(LOOPBACK_IP)
        ) {
            emulator.start();
            testBlob(emulator);
        }
    }

    @Test
    public void testWithQueueServiceClientWithSslUsingPfx() {
        try (
            AzuriteContainer emulator = new AzuriteContainer(
                DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.33.0")
            )
                .withSsl(MountableFile.forClasspathResource("/keystore.pfx"), PASSWORD)
                .withHost(LOOPBACK_IP)
        ) {
            emulator.start();
            testQueue(emulator);
        }
    }

    @Test
    public void testWithTableServiceClientWithSslUsingPfx() {
        try (
            AzuriteContainer emulator = new AzuriteContainer(
                DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.33.0")
            )
                .withSsl(MountableFile.forClasspathResource("/keystore.pfx"), PASSWORD)
                .withHost(LOOPBACK_IP)
        ) {
            emulator.start();
            testTable(emulator);
        }
    }

    @Test
    public void testWithBlobServiceClientWithSslUsingPem() {
        try (
            AzuriteContainer emulator = new AzuriteContainer(
                DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.33.0")
            )
                .withSsl(
                    MountableFile.forClasspathResource("/certificate.pem"),
                    MountableFile.forClasspathResource("/key.pem")
                )
                .withHost(LOOPBACK_IP)
        ) {
            emulator.start();
            testBlob(emulator);
        }
    }

    @Test
    public void testWithQueueServiceClientWithSslUsingPem() {
        try (
            AzuriteContainer emulator = new AzuriteContainer(
                DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.33.0")
            )
                .withSsl(
                    MountableFile.forClasspathResource("/certificate.pem"),
                    MountableFile.forClasspathResource("/key.pem")
                )
                .withHost(LOOPBACK_IP)
        ) {
            emulator.start();
            testQueue(emulator);
        }
    }

    @Test
    public void testWithTableServiceClientWithSslUsingPem() {
        try (
            AzuriteContainer emulator = new AzuriteContainer(
                DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite:3.33.0")
            )
                .withSsl(
                    MountableFile.forClasspathResource("/certificate.pem"),
                    MountableFile.forClasspathResource("/key.pem")
                )
                .withHost(LOOPBACK_IP)
        ) {
            emulator.start();
            testTable(emulator);
        }
    }

    private void testBlob(AzuriteContainer container) {
        // createBlobClient {
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(container.getDefaultConnectionString())
            .buildClient();
        // }
        BlobContainerClient containerClient = blobServiceClient.createBlobContainer("test-container");

        assertThat(containerClient.exists()).isTrue();
    }

    private void testQueue(AzuriteContainer container) {
        // createQueueClient {
        QueueServiceClient queueServiceClient = new QueueServiceClientBuilder()
            .connectionString(container.getDefaultConnectionString())
            .buildClient();
        // }
        QueueClient queueClient = queueServiceClient.createQueue("test-queue");

        assertThat(queueClient.getQueueUrl()).isNotNull();
    }

    private void testTable(AzuriteContainer container) {
        // createTableClient {
        TableServiceClient tableServiceClient = new TableServiceClientBuilder()
            .connectionString(container.getDefaultConnectionString())
            .buildClient();
        // }
        TableClient tableClient = tableServiceClient.createTable("testtable");

        assertThat(tableClient.getTableEndpoint()).isNotNull();
    }
}

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
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;

public class AzuriteContainerTest {

    private static final File PFX_STORE_FILE = getResourceFile("/keystore.pfx");

    private static final File PEM_CERT_FILE = getResourceFile("/certificate.pem");

    private static final File PEM_KEY_FILE = getResourceFile("/key.pem");

    private static final String PASSWORD = "changeit";

    private static final String LOOPBACK_IP = "127.0.0.1";

    private static Properties originalSystemProperties;

    @BeforeClass
    public static void captureOriginalSystemProperties() {
        originalSystemProperties = (Properties) System.getProperties().clone();
        System.setProperty("javax.net.ssl.trustStore", PFX_STORE_FILE.getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", PASSWORD);
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
    }

    @AfterClass
    public static void restoreOriginalSystemProperties() {
        System.setProperties(originalSystemProperties);
    }

    @Rule
    // emulatorContainer {
    public AzuriteContainer emulator = new AzuriteContainer(
        DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite")
    );

    // }

    @Rule
    public AzuriteContainer pfxEmulator = new AzuriteContainer(
        DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite")
    )
        .withSsl(PFX_STORE_FILE, PASSWORD)
        .withHost(LOOPBACK_IP);

    @Rule
    public AzuriteContainer pemEmulator = new AzuriteContainer(
        DockerImageName.parse("mcr.microsoft.com/azure-storage/azurite")
    )
        .withSsl(PEM_CERT_FILE, PEM_KEY_FILE)
        .withHost(LOOPBACK_IP);

    @Test
    public void testWithBlobServiceClient() {
        // getConnectionString {
        final String connectionString = emulator.getDefaultConnectionString();
        // }
        testBlob(connectionString);
    }

    @Test
    public void testWithQueueServiceClient() {
        final String connectionString = emulator.getDefaultConnectionString();
        testQueue(connectionString);
    }

    @Test
    public void testWithTableServiceClient() {
        final String connectionString = emulator.getDefaultConnectionString();
        testTable(connectionString);
    }

    @Test
    public void testWithBlobServiceClientWithSslUsingPfx() {
        final String connectionString = pfxEmulator.getDefaultConnectionString();
        testBlob(connectionString);
    }

    @Test
    public void testWithQueueServiceClientWithSslUsingPfx() {
        final String connectionString = pfxEmulator.getDefaultConnectionString();
        testQueue(connectionString);
    }

    @Test
    public void testWithTableServiceClientWithSslUsingPfx() {
        final String connectionString = pfxEmulator.getDefaultConnectionString();
        testTable(connectionString);
    }

    @Test
    public void testWithBlobServiceClientWithSslUsingPem() {
        final String connectionString = pemEmulator.getDefaultConnectionString();
        testBlob(connectionString);
    }

    @Test
    public void testWithQueueServiceClientWithSslUsingPem() {
        final String connectionString = pemEmulator.getDefaultConnectionString();
        testQueue(connectionString);
    }

    @Test
    public void testWithTableServiceClientWithSslUsingPem() {
        final String connectionString = pemEmulator.getDefaultConnectionString();
        testTable(connectionString);
    }

    private void testBlob(final String connectionString) {
        // createBlobClient {
        final BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();
        // }
        final BlobContainerClient containerClient = blobServiceClient.createBlobContainer("test-container");

        Assert.assertTrue(containerClient.exists());
    }

    private void testQueue(final String connectionString) {
        // createQueueClient {
        final QueueServiceClient queueServiceClient = new QueueServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();
        // }
        final QueueClient queueClient = queueServiceClient.createQueue("test-queue");

        Assert.assertNotNull(queueClient.getQueueUrl());
    }

    private void testTable(final String connectionString) {
        // createTableClient {
        final TableServiceClient tableServiceClient = new TableServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();
        // }
        final TableClient tableClient = tableServiceClient.createTable("testtable");

        Assert.assertNotNull(tableClient.getTableEndpoint());
    }

    private static File getResourceFile(final String resourceName) {
        final URL resource = AzuriteContainerTest.class.getResource(resourceName);
        return Optional.ofNullable(resource).map(URL::getFile).map(File::new).orElse(null);
    }
}

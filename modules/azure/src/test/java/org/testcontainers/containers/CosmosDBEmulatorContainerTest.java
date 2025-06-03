package org.testcontainers.containers;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class CosmosDBEmulatorContainerTest {

    private static Properties originalSystemProperties;

    @BeforeAll
    public static void captureOriginalSystemProperties() {
        originalSystemProperties = (Properties) System.getProperties().clone();
    }

    @AfterAll
    public static void restoreOriginalSystemProperties() {
        System.setProperties(originalSystemProperties);
    }

    @TempDir
    public Path tempFolder;

    // emulatorContainer {
    @Container
    public CosmosDBEmulatorContainer emulator = new CosmosDBEmulatorContainer(
        DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest")
    );

    // }

    @Test
    public void testWithCosmosClient() throws Exception {
        // buildAndSaveNewKeyStore {
        Path keyStoreFile = tempFolder.resolve("azure-cosmos-emulator.keystore");
        KeyStore keyStore = emulator.buildNewKeyStore();
        keyStore.store(new FileOutputStream(keyStoreFile.toFile()), emulator.getEmulatorKey().toCharArray());
        // }
        // setSystemTrustStoreParameters {
        System.setProperty("javax.net.ssl.trustStore", keyStoreFile.toString());
        System.setProperty("javax.net.ssl.trustStorePassword", emulator.getEmulatorKey());
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
        // }
        // buildClient {
        CosmosAsyncClient client = new CosmosClientBuilder()
            .gatewayMode()
            .endpointDiscoveryEnabled(false)
            .endpoint(emulator.getEmulatorEndpoint())
            .key(emulator.getEmulatorKey())
            .buildAsyncClient();
        // }
        // testWithClientAgainstEmulatorContainer {
        CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists("Azure").block();
        assertThat(databaseResponse.getStatusCode()).isEqualTo(201);
        CosmosContainerResponse containerResponse = client
            .getDatabase("Azure")
            .createContainerIfNotExists("ServiceContainer", "/name")
            .block();
        assertThat(containerResponse.getStatusCode()).isEqualTo(201);
        // }
    }
}

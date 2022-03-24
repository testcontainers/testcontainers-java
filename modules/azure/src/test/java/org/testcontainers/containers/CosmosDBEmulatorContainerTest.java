package org.testcontainers.containers;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.utility.DockerImageName;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Properties;

public class CosmosDBEmulatorContainerTest {

    private static Properties originalSystemProperties;

    @BeforeClass
    public static void captureOriginalSystemProperties() {
        originalSystemProperties = (Properties) System.getProperties().clone();
    }

    @AfterClass
    public static void restoreOriginalSystemProperties() {
        System.setProperties(originalSystemProperties);
    }

    @Rule
    public TemporaryFolder tempFolder = TemporaryFolder.builder().assureDeletion().build();

    @Rule
    // emulatorContainer {
    public CosmosDBEmulatorContainer emulator = new CosmosDBEmulatorContainer(
            DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator:latest")
    );
    // }

    @Test
    public void testWithCosmosClient() throws Exception {
        // buildAndSaveNewKeyStore {
        Path keyStoreFile = tempFolder.newFile("azure-cosmos-emulator.keystore").toPath();
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
        CosmosDatabaseResponse databaseResponse =
                client.createDatabaseIfNotExists("Azure").block();
        Assertions.assertThat(databaseResponse.getStatusCode()).isEqualTo(201);
        CosmosContainerResponse containerResponse =
                client.getDatabase("Azure").createContainerIfNotExists("ServiceContainer", "/name").block();
        Assertions.assertThat(containerResponse.getStatusCode()).isEqualTo(201);
        // }
    }
}

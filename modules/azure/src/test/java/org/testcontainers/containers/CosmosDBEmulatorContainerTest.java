package org.testcontainers.containers;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

public class CosmosDBEmulatorContainerTest {

    @Rule
    // emulatorContainer {
    public CosmosDBEmulatorContainer emulator = new CosmosDBEmulatorContainer(
        DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator")
    );
    // }

    @Test
    public void testSimple() throws Exception {
    // buildAndSaveNewKeyStore {
        Path keyStoreFile = Files.createTempFile("emulator", ".keystore");
        KeyStore keyStore = emulator.buildNewKeyStore("/tmp/cosmos/appdata/default.sslcert.pfx");
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
            .endpoint(this.emulator.getEmulatorEndpoint())
            .key(this.emulator.getEmulatorKey())
            .buildAsyncClient();
    // }
    // testWithClientAgainstEmulatorContainer {
        CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists("Azure")
                                                        .block();
        assertThat(databaseResponse.getStatusCode()).isEqualTo(201);
        CosmosContainerResponse containerResponse = client.getDatabase("Azure")
                                                          .createContainerIfNotExists("ServiceContainer", "/name")
                                                          .block();
        assertThat(containerResponse.getStatusCode()).isEqualTo(201);
    // }
    }
}

package org.testcontainers.containers;

import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Onur Kagan Ozcan
 */
public class CosmosDBEmulatorContainerTest {

    @Rule
    // emulatorContainer {
    public CosmosDBEmulatorContainer emulator = new CosmosDBEmulatorContainer(
        DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator")
    );
    // }

    @Test
    // testWithEmulatorContainer {
    public void testSimple() {
        CosmosAsyncClient client = new CosmosClientBuilder()
            .gatewayMode()
            .endpointDiscoveryEnabled(false)
            .endpoint(emulator.getEmulatorEndpoint())
            .key(CosmosDBEmulatorContainer.EMULATOR_KEY)
            .buildAsyncClient();
        CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists("Azure")
                                                        .block();
        assertThat(databaseResponse.getStatusCode()).isEqualTo(201);
        
        CosmosContainerResponse containerResponse = client.getDatabase("Azure")
                                                          .createContainerIfNotExists("ServiceContainer", "/name")
                                                          .block();
        assertThat(containerResponse.getStatusCode()).isEqualTo(201);
    }
    // }
}

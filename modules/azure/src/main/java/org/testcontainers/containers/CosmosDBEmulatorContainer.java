package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.security.KeyStore;

/**
 * An Azure CosmosDB container
 */
public class CosmosDBEmulatorContainer extends GenericContainer<CosmosDBEmulatorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME =
            DockerImageName.parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator");

    private static final int PORT = 8081;

    /**
     * @param dockerImageName specified docker image name to run
     */
    public CosmosDBEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(PORT);
        waitingFor(Wait.forLogMessage("(?s).*Started\\r\\n$", 1));
    }

    /**
     * @return new KeyStore built with PKCS12
     */
    public KeyStore buildNewKeyStore() {
        return KeyStoreBuilder.buildByDownloadingCertificate(getEmulatorEndpoint(), getEmulatorKey());
    }

    /**
     * Emulator key is a known constant and specified in Azure Cosmos DB Documents.
     * This key is also used as password for emulator certificate file.
     *
     * @return predefined emulator key
     * @see <a href="https://docs.microsoft.com/en-us/azure/cosmos-db/local-emulator?tabs=ssl-netstd21#authenticate-requests">Azure Cosmos DB Documents</a>
     */
    public String getEmulatorKey() {
        return "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==";
    }

    /**
     * @return secure https emulator endpoint to send requests
     */
    public String getEmulatorEndpoint() {
        return "https://" + getHost() + ":" + getMappedPort(PORT);
    }
}

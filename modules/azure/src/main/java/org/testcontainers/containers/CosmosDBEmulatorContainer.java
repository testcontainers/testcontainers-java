package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.KeyStoreUtils.STORE_PASSWORD;
import static org.testcontainers.containers.KeyStoreUtils.STORE_TYPE;
import static org.testcontainers.containers.KeyStoreUtils.downloadPemFromEmulator;
import static org.testcontainers.containers.KeyStoreUtils.importEmulatorCertificate;
import static org.testcontainers.utility.DockerImageName.parse;

/**
 * An Azure CosmosDB container
 *
 * @author Onur Kagan Ozcan
 */
public class CosmosDBEmulatorContainer extends GenericContainer<CosmosDBEmulatorContainer> {

    /**
     * @link {https://docs.microsoft.com/en-us/azure/cosmos-db/local-emulator?tabs=ssl-netstd21#authenticate-requests}
     */
    public static final String EMULATOR_KEY =
        "C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVTqobD4b8mGGyPMbIZnqyMsEcaGQy67XIw/Jw==";

    private static final DockerImageName DEFAULT_IMAGE_NAME = parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator");

    private static final String TEMP_DIRECTORY_NAME = "azure-cosmosdb-emulator-temp";

    private static final String EMULATOR_CERTIFICATE_FILE_NAME = "emulator.pem";

    private static final String KEYSTORE_FILE_NAME = "cosmos_emulator.keystore";

    private static final int PORT = 8081;

    private Path tempDirectory;

    public CosmosDBEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(PORT);
        waitingFor(Wait.forLogMessage("(?s).*Started\\r\\n$", 1));
    }

    @Override
    protected void configure() {
        try {
            this.tempDirectory = Files.createTempDirectory(TEMP_DIRECTORY_NAME);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        Path emulatorCertificatePath = tempDirectory.resolve(EMULATOR_CERTIFICATE_FILE_NAME);
        downloadPemFromEmulator(getEmulatorEndpoint(), emulatorCertificatePath);
        Path keyStorePath = tempDirectory.resolve(KEYSTORE_FILE_NAME);
        importEmulatorCertificate(emulatorCertificatePath, keyStorePath);
        setSystemTrustStoreParameters(keyStorePath.toFile().getAbsolutePath());
    }

    /**
     * @param trustStore keyStore path
     */
    private void setSystemTrustStoreParameters(String trustStore) {
        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", STORE_PASSWORD);
        System.setProperty("javax.net.ssl.trustStoreType", STORE_TYPE);
    }

    /**
     * @return secure https emulator endpoint to send requests
     */
    public String getEmulatorEndpoint() {
        return "https://" + getHost() + ":" + getMappedPort(PORT);
    }
}

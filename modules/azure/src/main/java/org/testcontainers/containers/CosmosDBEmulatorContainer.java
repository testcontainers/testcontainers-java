package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.Constants.EMULATOR_CERTIFICATE_FILE_NAME;
import static org.testcontainers.containers.Constants.KEYSTORE_FILE_NAME;
import static org.testcontainers.containers.Constants.STORE_PASSWORD;
import static org.testcontainers.containers.Constants.STORE_TYPE;
import static org.testcontainers.containers.Constants.TEMP_DIRECTORY_NAME;
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

    private static final int PORT = 8081;

    private Path tempDirectory;

    private Boolean autoSetSystemTrustStoreParameters = true;

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
        KeyStoreUtils.downloadPemFromEmulator(getEmulatorEndpoint(), emulatorCertificatePath);
        Path keyStorePath = tempDirectory.resolve(KEYSTORE_FILE_NAME);
        KeyStoreUtils.importEmulatorCertificate(emulatorCertificatePath, keyStorePath);
        if (autoSetSystemTrustStoreParameters) {
            setSystemTrustStoreParameters(keyStorePath.toFile().getAbsolutePath(), STORE_PASSWORD, STORE_TYPE);
        }
    }

    /**
     * Disable system property set for further customizations.
     * You can still set with public method
     *
     * @see CosmosDBEmulatorContainer#setSystemTrustStoreParameters(String, String, String)
     * @return current instance
     */
    public CosmosDBEmulatorContainer withDisablingAutoSetSystemTrustStoreParameters() {
        this.autoSetSystemTrustStoreParameters = false;
        return this;
    }

    /**
     * @param trustStore keyStore path
     * @param trustStorePassword keyStore file password
     * @param trustStoreType keyStore type e.g PKCS12, JKS
     */
    public void setSystemTrustStoreParameters(String trustStore, String trustStorePassword, String trustStoreType) {
        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
        System.setProperty("javax.net.ssl.trustStoreType", trustStoreType);
    }

    /**
     * @return endpoint to use in CosmosClient/CosmosAsyncClient objects
     */
    public String getEmulatorEndpoint() {
        return "https://" + getHost() + ":" + getMappedPort(PORT);
    }
}

package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.Constants.EMULATOR_CERTIFICATE_FILE_NAME;
import static org.testcontainers.containers.Constants.EMULATOR_KEY;
import static org.testcontainers.containers.Constants.KEYSTORE_FILE_NAME;
import static org.testcontainers.containers.Constants.STORE_PASSWORD;
import static org.testcontainers.containers.Constants.STORE_TYPE;
import static org.testcontainers.containers.Constants.TEMP_DIRECTORY_NAME;
import static org.testcontainers.utility.DockerImageName.parse;

/**
 * An Azure CosmosDB container
 *
 * Default port is 8081.
 *
 * @author Onur Kagan Ozcan
 */
public class CosmosDBEmulatorContainer extends GenericContainer<CosmosDBEmulatorContainer> {

    public static final String LINUX_AZURE_COSMOS_DB_EMULATOR = "mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator";

    private static final DockerImageName DEFAULT_IMAGE_NAME = parse(LINUX_AZURE_COSMOS_DB_EMULATOR);

    private static final int PORT = 8081;

    private Path tempDirectory;

    private Boolean autoSetSystemTrustStoreParameters = true;

    public CosmosDBEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(PORT);
        setWaitStrategy(new LogMessageWaitStrategy().withRegEx("(?s).*Started\\r\\n$"));
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

    /**
     * @return default local key of emulator as defined in Azure Cosmos DB docs and examples
     */
    public String getEmulatorLocalKey() {
        return EMULATOR_KEY;
    }
}

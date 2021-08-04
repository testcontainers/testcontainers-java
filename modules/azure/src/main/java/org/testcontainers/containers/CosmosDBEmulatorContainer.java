package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.utility.DockerImageName.parse;

/**
 * An Azure CosmosDB container
 */
public class CosmosDBEmulatorContainer extends GenericContainer<CosmosDBEmulatorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = parse("mcr.microsoft.com/cosmosdb/linux/azure-cosmos-emulator");

    private static final int PORT = 8081;

    private Path tempDirectory;

    /**
     * @param dockerImageName specified docker image name to run
     */
    public CosmosDBEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(PORT);
        waitingFor(Wait.forLogMessage("(?s).*Started\\r\\n$", 1));
    }

    @Override
    protected void configure() {
        try {
            this.tempDirectory = Files.createTempDirectory("azure-cosmosdb-emulator-temp");
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        Path keyStorePath = prepareKeyStore();
        setSystemTrustStoreParameters(keyStorePath.toFile().getAbsolutePath());
    }

    /**
     * @return key store path
     */
    private Path prepareKeyStore() {
        String certFileName = "default.sslcert.pfx";
        Path certFilePath = tempDirectory.resolve(certFileName);
        copyFileFromContainer("/tmp/cosmos/appdata/" + certFileName, certFilePath.toString());
        Path keyStorePath = tempDirectory.resolve("cosmos_emulator.keystore");
        importEmulatorCertificate(certFilePath, keyStorePath);
        return keyStorePath;
    }

    private void importEmulatorCertificate(Path pfxLocation, Path keyStoreOutput) {
        try {
            KeyStore keystore = KeyStore.getInstance("PKCS12");
            keystore.load(new FileInputStream(pfxLocation.toFile()), getEmulatorKey().toCharArray());
            keystore.store(new FileOutputStream(keyStoreOutput.toFile()), getEmulatorKey().toCharArray());
        } catch (Exception ex) {
            throw new IllegalStateException();
        }
    }

    /**
     * @param trustStore keyStore path
     */
    private void setSystemTrustStoreParameters(String trustStore) {
        System.setProperty("javax.net.ssl.trustStore", trustStore);
        System.setProperty("javax.net.ssl.trustStorePassword", getEmulatorKey());
        System.setProperty("javax.net.ssl.trustStoreType", "PKCS12");
    }

    /**
     * EMULATOR_KEY is a known constant and specified in Azure Cosmos DB Documents as well.
     * Key value is also used as password for emulator certificate file.
     *
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

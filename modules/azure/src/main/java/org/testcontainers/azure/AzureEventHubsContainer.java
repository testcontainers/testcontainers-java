package org.testcontainers.azure;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.LicenseAcceptance;

/**
 * Testcontainers implementation for Azure Eventhubs Emulator.
 * <p>
 * Supported image: {@code "mcr.microsoft.com/azure-messaging/eventhubs-emulator"}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>AMQP: 5672</li>
 * </ul>
 */
public class AzureEventHubsContainer extends GenericContainer<AzureEventHubsContainer> {

    private static final int DEFAULT_AMQP_PORT = 5672;

    private static final String CONNECTION_STRING_FORMAT =
        "Endpoint=sb://%s:%d;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";

    private static final String BOOTSTRAP_SERVERS_FORMAT = "%s:%d";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "mcr.microsoft.com/azure-messaging/eventhubs-emulator"
    );

    private AzuriteContainer azuriteContainer;

    /**
     * @param dockerImageName specified docker image name to run
     */
    public AzureEventHubsContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * @param dockerImageName specified docker image name to run
     */
    public AzureEventHubsContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        waitingFor(Wait.forLogMessage(".*Emulator Service is Successfully Up!.*", 1));
        withExposedPorts(DEFAULT_AMQP_PORT);
    }

    /**
     * * Sets the Azurite dependency needed by the Event Hubs Container,
     *
     * @param azuriteContainer The Azurite container used by Event HUbs as a dependency
     * @return this
     */
    public AzureEventHubsContainer withAzuriteContainer(final AzuriteContainer azuriteContainer) {
        this.azuriteContainer = azuriteContainer;
        dependsOn(this.azuriteContainer);
        return this;
    }

    /**
     * Provide the broker configuration to the container.
     *
     * @param config The file containing the broker configuration
     * @return this
     */
    public AzureEventHubsContainer withConfig(final Transferable config) {
        withCopyToContainer(config, "/Eventhubs_Emulator/ConfigFiles/Config.json");
        return this;
    }

    /**
     * Accepts the EULA of the container.
     *
     * @return this
     */
    public AzureEventHubsContainer acceptLicense() {
        return withEnv("ACCEPT_EULA", "Y");
    }

    @Override
    protected void configure() {
        if (azuriteContainer == null) {
            throw new IllegalStateException(
                "The image " +
                getDockerImageName() +
                " requires an Azurite container. Please provide one with the withAzuriteContainer method!"
            );
        }
        final String azuriteHost = azuriteContainer.getNetworkAliases().get(0);
        withEnv("BLOB_SERVER", azuriteHost);
        withEnv("METADATA_SERVER", azuriteHost);
        // If license was not accepted programmatically, check if it was accepted via resource file
        if (!getEnvMap().containsKey("ACCEPT_EULA")) {
            LicenseAcceptance.assertLicenseAccepted(this.getDockerImageName());
            acceptLicense();
        }
    }

    /**
     * Returns the connection string.
     *
     * @return connection string
     */
    public String getConnectionString() {
        return String.format(CONNECTION_STRING_FORMAT, getHost(), getMappedPort(DEFAULT_AMQP_PORT));
    }
}

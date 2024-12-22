package org.testcontainers.azure;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Testcontainers implementation for Azure Eventhubs Emulator.
 * <p>
 * Supported image: {@code "mcr.microsoft.com/azure-messaging/eventhubs-emulator"}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>5672 (AMQP port)</li>
 *     <li>9092 (Kafka port)</li>
 * </ul>
 */
public class AzureEventhubsEmulatorContainer extends GenericContainer<AzureEventhubsEmulatorContainer> {

    private static final String DEFAULT_HOST = "127.0.0.1";

    private static final int DEFAULT_AMQP_PORT = 5672;

    private static final int DEFAULT_KAFKA_PORT = 9092;

    private static final String CONNECTION_STRING_FORMAT =
        "Endpoint=sb://%s:%d;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "mcr.microsoft.com/azure-messaging/eventhubs-emulator"
    );

    private String host = DEFAULT_HOST;

    private AzuriteContainer azuriteContainer;

    private MountableFile config;

    /**
     * @param dockerImageName specified docker image name to run
     */
    public AzureEventhubsEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        waitingFor(Wait.forLogMessage(".*Emulator Service is Successfully Up!.*", 1));
        withExposedPorts(DEFAULT_AMQP_PORT, DEFAULT_KAFKA_PORT);
    }

    @Override
    public void start() {
        if (azuriteContainer == null) {
            azuriteContainer =
                new AzuriteContainer(AzuriteContainer.DEFAULT_IMAGE_NAME.withTag("3.33.0")).withNetwork(getNetwork());
        }
        azuriteContainer.start();

        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        if (azuriteContainer != null) {
            azuriteContainer.stop();
        }
    }

    /**
     * Provide the broker configuration to the container.
     *
     * @param config The file containing the broker configuration
     * @return this
     */
    public AzureEventhubsEmulatorContainer withConfig(final MountableFile config) {
        this.config = config;
        return this;
    }

    /**
     * Sets the hostname we want to use to connect to our emulator. (default: {@link #DEFAULT_HOST})
     *
     * @param host The host name
     * @return this
     */
    public AzureEventhubsEmulatorContainer withHost(final String host) {
        this.host = host;
        return this;
    }

    /**
     * Accepts the EULA of the container.
     *
     * @return this
     */
    public AzureEventhubsEmulatorContainer acceptEula() {
        return withEnv("ACCEPT_EULA", "Y");
    }

    @Override
    protected void configure() {
        dependsOn(azuriteContainer);
        final String azuriteHost = azuriteContainer.getNetworkAliases().get(0);
        withEnv("BLOB_SERVER", azuriteHost);
        withEnv("METADATA_SERVER", azuriteHost);
        if (config != null) {
            logger().info("Using path for configuration file: '{}'", config);
            withCopyFileToContainer(config, "/Eventhubs_Emulator/ConfigFiles/Config.json");
        }
    }

    /**
     * Returns the connection string.
     *
     * @return connection string
     */
    public String getConnectionString() {
        return String.format(CONNECTION_STRING_FORMAT, host, getMappedPort(DEFAULT_AMQP_PORT));
    }
}

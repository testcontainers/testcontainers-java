package org.testcontainers.azure;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Testcontainers implementation for Azure service bus emulator.
 *  <p>
 *  Supported image: {@code mcr.microsoft.com/azure-messaging/servicebus-emulator}
 *  </p>
 *  <p> Exposed ports: 5672</p>
 *  <p>
 *   If the official client, azure-messaging-servicebus, is used the oldest version supported is 7.17.8.
 *  </p>
 *  <p>
 *  By default, emulator uses <a href="https://github.com/Azure/azure-service-bus-emulator-installer/blob/main/ServiceBus-Emulator/Config/Config.json">config.json</a> configuration file.
 *  To supply your own config your own config using {@code withConfigFile(MountableFile)}
 * </p>
 * <p>
 *     The service bus emulator requires a database, so a {@code MSSQLServerContainer} is also started.
 * </p>
 */
public class AzureServicebusEmulatorContainer extends GenericContainer<AzureServicebusEmulatorContainer> {
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "mcr.microsoft.com/azure-messaging/servicebus-emulator"
    );

    private static final int PORT = 5672;

    /**
     * @param dockerImageName specified docker image name to run
     */
    public AzureServicebusEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(PORT);

        waitingFor(Wait.forLogMessage(".*Emulator Service is Successfully Up!.*", 1));

        withNetwork(Network.SHARED);
        withNetworkAliases("sb-emulator");
        MSSQLServerContainer mssqlServerContainer = new MSSQLServerContainer(DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-CU14-ubuntu-22.04"))
            .acceptLicense();
        String mssqlNetworkAlias = "sqlserver";
        dependsOn(
             mssqlServerContainer
                 .withNetwork(Network.SHARED)
                 .withNetworkAliases(mssqlNetworkAlias)
        );
        addEnv("MSSQL_SA_PASSWORD", mssqlServerContainer.getPassword());
        addEnv("SQL_SERVER", mssqlNetworkAlias);
        acceptLicense();
    }

    /**
     * @param configFile <a href="https://github.com/Azure/azure-service-bus-emulator-installer/blob/main/ServiceBus-Emulator/Config/Config.json">config.json</a>
     * @return this
     */
    public AzureServicebusEmulatorContainer withConfigFile(MountableFile configFile) {
        return withCopyFileToContainer(
            configFile,
            "/ServiceBus_Emulator/ConfigFiles/Config.json"
        );
    }

    /**
     * Accepts the license for the Azure Service Bus Emulator container by setting the ACCEPT_EULA=Y
     * variable as described at <a href="https://github.com/Azure/azure-service-bus-emulator-installer/blob/main/README.md">https://github.com/Azure/azure-service-bus-emulator-installer/blob/main/README.md#license</a>
     */
    public AzureServicebusEmulatorContainer acceptLicense() {
        addEnv("ACCEPT_EULA", "Y");
        return self();
    }

    /**
     * @return connection string for connecting to the service bus.
     */
    public String getConnectionString() {
        Integer mappedPort = getMappedPort(5672);
        return "Endpoint=sb://localhost:" + mappedPort + ";SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";
    }
}

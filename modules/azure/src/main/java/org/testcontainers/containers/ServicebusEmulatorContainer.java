package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class ServicebusEmulatorContainer<SELF extends ServicebusEmulatorContainer<SELF>> extends GenericContainer<SELF> {
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "mcr.microsoft.com/azure-messaging/servicebus-emulator"
    );

    private static final int PORT = 5672;

    /**
     * @param dockerImageName specified docker image name to run
     */
    public ServicebusEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(PORT);

        waitingFor(Wait.forLogMessage(".*Emulator Service is Successfully Up!.*", 1));

        withNetwork(Network.SHARED);
        withNetworkAliases("sb-emulator");
        MSSQLServerContainer mssqlServerContainer = new MSSQLServerContainer(DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-CU14-ubuntu-22.04"))
            .acceptLicense();
        String mssqlNetworkAlias = "sqledge";
        dependsOn(
             mssqlServerContainer
                 .withNetwork(Network.SHARED)
                 .withNetworkAliases(mssqlNetworkAlias)
        );
        addEnv("MSSQL_SA_PASSWORD", mssqlServerContainer.getPassword());
        addEnv("SQL_SERVER", mssqlNetworkAlias);
        acceptLicense();
    }

    public SELF withConfigFile(MountableFile configFile) {
        return withCopyFileToContainer(
            configFile,
            "/ServiceBus_Emulator/ConfigFiles/Config.json"
        );
    }

    /**
     * Accepts the license for the Azure Service Bus Emulator container by setting the ACCEPT_EULA=Y
     * variable as described at <a href="https://github.com/Azure/azure-service-bus-emulator-installer/blob/main/README.md">https://github.com/Azure/azure-service-bus-emulator-installer/blob/main/README.md#license</a>
     */
    public SELF acceptLicense() {
        addEnv("ACCEPT_EULA", "Y");
        return self();
    }

    public String getConnectionString() {
        Integer mappedPort = getMappedPort(5672);
        return "Endpoint=sb://localhost:" + mappedPort + ";SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=SAS_KEY_VALUE;UseDevelopmentEmulator=true;";
    }
}

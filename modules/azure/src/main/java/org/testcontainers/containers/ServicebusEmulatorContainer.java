package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class ServicebusEmulatorContainer extends GenericContainer<ServicebusEmulatorContainer> {
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "mcr.microsoft.com/azure-messaging/servicebus-emulator"
    );

    private static final int PORT = 1433;

    /**
     * @param dockerImageName specified docker image name to run
     */
    public ServicebusEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(PORT);
        waitingFor(Wait.forLogMessage(".*Emulator Service is Successfully Up!\\r\\n$", 1));
        Network network = Network.newNetwork();
        withNetwork(network);
        withNetworkAliases("sb-emulator");
        dependsOn(
             new SqlEdgeContainer(DockerImageName.parse("mcr.microsoft.com/azure-sql-edge:latest"))
                 .withNetwork(network)
                 .withNetworkAliases("sqledge")
        );
        addEnv("ACCEPT_EULA", "Y");
        addEnv("CONFIG_PATH", "/home/marv/Downloads/asb/config.json");
    }
}

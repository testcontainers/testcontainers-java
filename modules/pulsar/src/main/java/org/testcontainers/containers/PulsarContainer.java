package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * This container wraps Apache pulsar running in stanalone mode
 */
public class PulsarContainer extends GenericContainer<PulsarContainer> {

    public static final int PULSAR_PORT = 6850;

    public PulsarContainer() {
        this("2.1.0-incubating");
    }

    public PulsarContainer(String pulsarVersion) {
        super(TestcontainersConfiguration.getInstance().getPulsarImage() + ":" + pulsarVersion);
        withExposedPorts(PULSAR_PORT);
        withCommand("/bin/bash", "-c", "" +
            "servicePort=6850 webServicePort=8280 webServicePortTls=8643 bin/apply-config-from-env.py conf/proxy.conf && " +
            "bin/pulsar standalone & " +
            "bin/pulsar proxy --zookeeper-servers localhost:2181 --global-zookeeper-servers localhost:2181"
        );

        waitingFor(Wait.forLogMessage(".*messaging service is ready.*\\s", 1));
    }

    public String getPulsarBrokerUrl() {
        return String.format("pulsar://%s:%s", this.getContainerIpAddress(), this.getFirstMappedPort());
    }

}

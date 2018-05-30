package org.testcontainers.containers;

import org.testcontainers.utility.Base58;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.stream.Stream;

/**
 * This container wraps Apache pulsar running in stanalone mode
 */
public class PulsarContainer extends GenericContainer<PulsarContainer> {

    public static final int PULSAR_PORT = 6850;

    public PulsarContainer() {
        this("2.0.0-rc1-incubating");
    }

    public PulsarContainer(String pulsarVersion) {
        super(TestcontainersConfiguration.getInstance().getPulsarImage() + ":" + pulsarVersion);
        withExposedPorts(PULSAR_PORT);
        withClasspathResourceMapping("proxy.conf", "/pulsar/conf/proxy.conf", BindMode.READ_ONLY);
        withCommand("/bin/bash", "-c", "bin/pulsar standalone & bin/pulsar proxy --zookeeper-servers localhost:2181 --global-zookeeper-servers localhost:2181");
    }

    public String getPulsarBrokerUrl() {
        return String.format("pulsar://%s:%s", this.getContainerIpAddress(), this.getFirstMappedPort());
    }

}

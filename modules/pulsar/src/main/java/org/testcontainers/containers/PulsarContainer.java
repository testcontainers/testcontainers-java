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

        withNetwork(Network.newNetwork());
        String networkAlias = "pulsar-" + Base58.randomString(6);
        withNetworkAliases(networkAlias);
        withExposedPorts(PULSAR_PORT);

    }

    public String getPulsarBrokerUrl() {
        return String.format("pulsar://%s:%s", this.getContainerIpAddress(), this.getFirstMappedPort());
    }

    @Override
    public void start() {
        withClasspathResourceMapping("proxy.conf", "/pulsar/conf/proxy.conf", BindMode.READ_ONLY);
        withCommand("/bin/bash", "-c", "bin/pulsar standalone & bin/pulsar proxy --zookeeper-servers localhost:2181 --global-zookeeper-servers localhost:2181");
        super.start();
    }

    @Override
    public void stop() {
        Stream.<Runnable>of(super::stop).parallel().forEach(Runnable::run);
    }
}

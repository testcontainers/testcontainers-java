package org.testcontainers.junit4;

import org.junit.rules.ExternalResource;
import org.testcontainers.containers.Network;

public class NetworkRule extends ExternalResource {

    private final Network network;

    public NetworkRule(Network network) {
        this.network = network;
    }

    @Override
    protected void after() {
        network.close();
    }

    public Network get() {
        return network;
    }
}

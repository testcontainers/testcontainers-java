package org.testcontainers.junit4.rules;

import org.junit.rules.ExternalResource;
import org.testcontainers.containers.Network;

public class NetworkRule extends ExternalResource {

    private final Network network;

    NetworkRule(Network network) {
        this.network = network;
    }

    @Override
    protected void after() {
        this.network.close();
    }
}

package org.testcontainers.test;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.github.dockerjava.api.DockerClient;
import org.testcontainers.shaded.com.github.dockerjava.api.command.InspectContainerResponse;

/**
 * Created by novy on 14.01.17.
 */

class Network {

    interface CanPingContainers {
        default PingResponse ping(GenericContainer container) {
            final String containerIP = Network.ipAddressOf(container);
            try {
                final long start = System.currentTimeMillis();
                Network.ping(containerIP);
                final long end = System.currentTimeMillis();
                return new PingResponse(true, end - start);
            } catch (Exception ignored) {
                return new PingResponse(false, -1);
            }
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class PingResponse {
        private final boolean successful;
        private final long latency;

        boolean wasSuccessful() {
            return successful;
        }

        long latencyInMilliseconds() {
            return latency;
        }
    }

    static String ipAddressOf(GenericContainer container) {
        final DockerClient client = DockerClientFactory.instance().client();
        final InspectContainerResponse inspected = client.inspectContainerCmd(container.getContainerId()).exec();
        return inspected.getNetworkSettings().getNetworks().get("bridge").getIpAddress();
    }

    static void ping(String address) throws Exception {
        Runtime.getRuntime().exec("ping " + address + " -c 1").waitFor();
    }
}



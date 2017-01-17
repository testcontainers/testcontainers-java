package org.testcontainers.test;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.github.dockerjava.api.DockerClient;
import org.testcontainers.shaded.com.github.dockerjava.api.command.InspectContainerResponse;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by novy on 14.01.17.
 */

class Network {

    interface CanPingContainers {

        default PingResponse ping(GenericContainer container) {
            return ping(container, 56);
        }

        default PingResponse ping(GenericContainer container, long packetSizeInBytes) {
            final String containerIP = Network.ipAddressOf(container);
            return Network.ping(containerIP, packetSizeInBytes);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class PingResponse {
        private final String responseContent;

        boolean wasSuccessful() {
            return !unknownHost() && !packetLost();
        }

        private boolean unknownHost() {
            return lastLineOfResponse().contains("ping: unknown host");
        }

        private boolean packetLost() {
            return lastLineOfResponse().contains("100% packet loss");
        }

        double latencyInMilliseconds() {
            return Double.parseDouble(extractResponseTimeAsString());
        }

        private String extractResponseTimeAsString() {
            final Pattern minResponseTimePattern = Pattern.compile("rtt min/avg/max/mdev = ([+-]?([0-9]*[.])?[0-9]+)");
            final Matcher matcher = minResponseTimePattern.matcher(lastLineOfResponse());
            matcher.find();
            return matcher.group(1);
        }

        private String lastLineOfResponse() {
            final String[] responseLines = responseContent.split("\n");
            return new LinkedList<>(Arrays.asList(responseLines)).getLast();
        }
    }

    static String ipAddressOf(GenericContainer container) {
        final DockerClient client = DockerClientFactory.instance().client();
        final InspectContainerResponse inspected = client.inspectContainerCmd(container.getContainerId()).exec();
        return inspected.getNetworkSettings().getNetworks().get("bridge").getIpAddress();
    }

    @SneakyThrows
    static PingResponse ping(String address, long packetSizeInBytes) {
        final Process process = Runtime.getRuntime().exec(String.format("ping %s -c 1 -s %d", address, packetSizeInBytes));
        return new PingResponse(
                IOUtils.toString(process.getInputStream(), "UTF-8")
        );
    }
}



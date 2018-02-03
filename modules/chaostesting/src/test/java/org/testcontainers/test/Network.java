package org.testcontainers.test;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by novy on 14.01.17.
 */

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class Network implements CanSpawnExampleContainers {

    interface CanPingContainers {

        default PingResponse ping(GenericContainer container) {
            return ping(container, 56);
        }

        default PingResponse ping(GenericContainer container, long packetSizeInBytes) {
            return new Network().ping(container, packetSizeInBytes);
        }
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    static class PingResponse {
        private final String responseContent;

        boolean packetLost() {
            return lastLineOfResponse().contains("100% packet loss");
        }

        double latencyInMilliseconds() {
            return Double.parseDouble(extractResponseTimeAsString());
        }

        private String extractResponseTimeAsString() {
            Preconditions.checkArgument(wasSuccessful(), "Trying to extract latency from unsuccessful response");
            final Pattern minResponseTimePattern = Pattern.compile(
                    "round-trip min/avg/max = ([0-9]*[.]?[0-9]+)/[0-9]*[.]?[0-9]+/[0-9]*[.]?[0-9]+ ms"
            );
            final Matcher matcher = minResponseTimePattern.matcher(lastLineOfResponse());
            Preconditions.checkArgument(matcher.find(), "Latency pattern not found in response");
            return matcher.group(1);
        }

        private boolean wasSuccessful() {
            return !unknownHost() && !packetLost();
        }

        private boolean unknownHost() {
            return lastLineOfResponse().contains("ping: unknown host");
        }

        private String lastLineOfResponse() {
            final String[] responseLines = responseContent.split("\n");
            return new LinkedList<>(Arrays.asList(responseLines)).getLast();
        }
    }

    @SneakyThrows
    PingResponse ping(GenericContainer container, long packetSizeInBytes) {
        final GenericContainer pinger = startedContainer();
        final Container.ExecResult pingResponse = pinger.execInContainer(
                "sh", "-c", String.format("ping %s -c 1 -s %d", ipAddressOf(container), packetSizeInBytes)
        );
        return new PingResponse(pingResponse.getStdout());
    }

    private String ipAddressOf(GenericContainer container) {
        final InspectContainerResponse inspected = dockerClient().inspectContainerCmd(container.getContainerId()).exec();
        return inspected.getNetworkSettings().getNetworks().get("bridge").getIpAddress();
    }
}



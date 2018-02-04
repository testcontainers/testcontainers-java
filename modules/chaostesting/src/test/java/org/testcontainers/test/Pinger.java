package org.testcontainers.test;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Pinger {

    private final Container delegate;

    Pinger() {
        delegate = new Container();
    }

    void start() {
        delegate.start();
    }

    PingResponse ping(Container container) {
        return ping(container, 16);
    }

    @SneakyThrows
    PingResponse ping(Container container, long packetSizeInBytes) {
        final org.testcontainers.containers.Container.ExecResult pingResponse = delegate.execInContainer(
                "sh", "-c", String.format("ping %s -c 1 -s %d", container.ipAddress(), packetSizeInBytes)
        );
        return new PingResponse(pingResponse.getStdout());
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
}

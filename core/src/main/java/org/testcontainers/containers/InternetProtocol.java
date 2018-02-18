package org.testcontainers.containers;

/**
 * The IP protocols supported by Docker.
 */
public enum InternetProtocol {

    TCP,
    UDP;

    public String toDockerNotation() {
        return name().toLowerCase();
    }

    public static InternetProtocol fromDockerNotation(String protocol) {
        return valueOf(protocol.toUpperCase());
    }
}

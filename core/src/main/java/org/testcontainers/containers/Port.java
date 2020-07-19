package org.testcontainers.containers;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Port {
    private final int value;
    private final InternetProtocol internetProtocol;

    private Port(int value, InternetProtocol internetProtocol) {
        this.value = value;
        this.internetProtocol = internetProtocol;
    }

    public com.github.dockerjava.api.model.ExposedPort toDockerExposedPort() {
        return new com.github.dockerjava.api.model.ExposedPort(this.value, com.github.dockerjava.api.model.InternetProtocol.parse(this.internetProtocol.toDockerNotation()));
    }

    public static Port of(int port, InternetProtocol internetProtocol) {
        return new Port(port, internetProtocol);
    }

    public static Port tcp(int port) {
        return Port.of(port, InternetProtocol.TCP);
    }

    public static Port udp(int port) {
        return Port.of(port, InternetProtocol.UDP);
    }

    public int getValue() {
        return value;
    }

    public InternetProtocol getInternetProtocol() {
        return internetProtocol;
    }

    @Override
    public String toString() {
        return "Port{" +
            "value=" + value +
            ", internetProtocol=" + internetProtocol +
            '}';
    }
}

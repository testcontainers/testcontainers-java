package org.testcontainers.junit;

import org.junit.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.io.File;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerComposeUdpExposedServiceTest {

    private final int servicePort = 33333;
    private final String serviceName = "udp-server";
    private final File dockerComposeFile = Paths.get("src/test/resources/udp/docker-compose-udp.yml").toFile();
    private LogMessageWaitStrategy logMessageWaitStrategy = new LogMessageWaitStrategy().withRegEx(".*\\bServer listening\\b.*");

    @Test
    public void serviceShouldBeReachableFromHostOnUdp() {
        try (DockerComposeContainer<?> dockerComposeContainer = new DockerComposeContainer<>(dockerComposeFile)
            .withExposedService(serviceName, servicePort, InternetProtocol.UDP, logMessageWaitStrategy)) {
            dockerComposeContainer.start();
            assertThat(dockerComposeContainer.getServicePort(serviceName, servicePort, InternetProtocol.UDP)).isNotNull();

        }
    }

    @Test
    public void udpAndTcpServicesShouldBeReachable() {
        File dockerComposeFile = Paths.get("src/test/resources/udp/docker-compose-udp-tcp-services.yml").toFile();
        try (DockerComposeContainer<?> dockerComposeContainer = new DockerComposeContainer<>(dockerComposeFile)
            .withExposedService(serviceName, servicePort, InternetProtocol.UDP, logMessageWaitStrategy)
            .withExposedService("redis", 6379)
        ) {
            dockerComposeContainer.start();
            assertThat(dockerComposeContainer.getServicePort(serviceName, servicePort, InternetProtocol.UDP)).isNotNull();
            assertThat(dockerComposeContainer.getServicePort("redis", 6379)).isNotNull();

        }
    }
}

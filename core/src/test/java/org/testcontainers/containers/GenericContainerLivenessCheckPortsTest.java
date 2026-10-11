package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.testcontainers.utility.DockerImageName;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class GenericContainerLivenessCheckPortsTest {

    @Test
    void udpOnlyPortsAreNotUsedForLivenessChecks() {
        GenericContainer<?> container = startedWithBindings(
            binding(12345, com.github.dockerjava.api.model.InternetProtocol.UDP, 54321)
        )
            .withExposedPort(12345, InternetProtocol.UDP);

        assertThat(container.getLivenessCheckPortNumbers()).isEmpty();
    }

    @Test
    void tcpPortsAreStillUsedWhenUdpPortsAreAlsoExposed() {
        GenericContainer<?> container = startedWithBindings(
            binding(12345, com.github.dockerjava.api.model.InternetProtocol.UDP, 54321),
            binding(8080, com.github.dockerjava.api.model.InternetProtocol.TCP, 58080)
        )
            .withExposedPort(12345, InternetProtocol.UDP)
            .withExposedPorts(8080);

        assertThat(container.getLivenessCheckPortNumbers()).containsExactly(58080);
    }

    @SafeVarargs
    private static GenericContainer<?> startedWithBindings(Map.Entry<ExposedPort, Ports.Binding[]>... bindings) {
        Map<ExposedPort, Ports.Binding[]> bindingMap = new HashMap<>();
        for (Map.Entry<ExposedPort, Ports.Binding[]> binding : bindings) {
            bindingMap.put(binding.getKey(), binding.getValue());
        }
        InspectContainerResponse containerInfo = Mockito.mock(InspectContainerResponse.class, Answers.RETURNS_DEEP_STUBS);
        when(containerInfo.getNetworkSettings().getPorts().getBindings()).thenReturn(bindingMap);
        return new StartedContainer(containerInfo);
    }

    private static class StartedContainer extends GenericContainer<StartedContainer> {

        private final InspectContainerResponse containerInfo;

        StartedContainer(InspectContainerResponse containerInfo) {
            super(DockerImageName.parse("alpine:3.20"));
            this.containerInfo = containerInfo;
        }

        @Override
        public String getContainerId() {
            return "container-id";
        }

        @Override
        public InspectContainerResponse getContainerInfo() {
            return containerInfo;
        }
    }

    private static Map.Entry<ExposedPort, Ports.Binding[]> binding(
        int port,
        com.github.dockerjava.api.model.InternetProtocol protocol,
        int hostPort
    ) {
        return new AbstractMap.SimpleEntry<>(
            new ExposedPort(port, protocol),
            new Ports.Binding[] { Ports.Binding.bindPort(hostPort) }
        );
    }
}

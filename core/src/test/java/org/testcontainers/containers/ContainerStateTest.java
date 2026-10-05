package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.NetworkSettings;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContainerStateTest {

    public static Object[][] params() {
        return new Object[][] {
            new Object[] { "regular mapping", "80:8080/tcp", Collections.singletonList(80) },
            new Object[] { "regular mapping with host", "127.0.0.1:80:8080/tcp", Collections.singletonList(80) },
            new Object[] { "zero port without host", ":0:8080/tcp", Collections.emptyList() },
            new Object[] { "missing port with host", "0.0.0.0:0:8080/tcp", Collections.emptyList() },
            new Object[] { "zero port (synthetic case)", "0:8080/tcp", Collections.emptyList() },
            new Object[] { "missing port", ":8080/tcp", Collections.emptyList() },
        };
    }

    @ParameterizedTest(name = "{0} ({1} -> {2})")
    @MethodSource("params")
    void test(String name, String testSet, List<Integer> expectedResult) {
        ContainerState containerState = mock(ContainerState.class);
        doCallRealMethod().when(containerState).getBoundPortNumbers();

        when(containerState.getPortBindings()).thenReturn(Collections.singletonList(testSet));

        List<Integer> result = containerState.getBoundPortNumbers();
        assertThat(result).hasSameElementsAs(expectedResult);
    }

    @Test
    void shouldGetMappedPortForUdpProtocol() {
        ContainerState containerState = mock(ContainerState.class);
        InspectContainerResponse containerInfo = mock(InspectContainerResponse.class);
        NetworkSettings networkSettings = mock(NetworkSettings.class);
        Ports ports = mock(Ports.class);

        // Set up the mock port bindings for UDP port 5353
        Map<ExposedPort, Ports.Binding[]> bindings = new HashMap<>();
        ExposedPort udpPort = new ExposedPort(5353, com.github.dockerjava.api.model.InternetProtocol.UDP);
        bindings.put(udpPort, new Ports.Binding[] { Ports.Binding.bindPort(12345) });

        when(containerState.getContainerId()).thenReturn("test-container-id");
        when(containerState.getContainerInfo()).thenReturn(containerInfo);
        when(containerInfo.getNetworkSettings()).thenReturn(networkSettings);
        when(networkSettings.getPorts()).thenReturn(ports);
        when(ports.getBindings()).thenReturn(bindings);
        doCallRealMethod().when(containerState).getMappedPort(5353, InternetProtocol.UDP);

        Integer mappedPort = containerState.getMappedPort(5353, InternetProtocol.UDP);
        assertThat(mappedPort).isEqualTo(12345);
    }

    @Test
    void shouldGetMappedPortForTcpUsingProtocol() {
        ContainerState containerState = mock(ContainerState.class);
        InspectContainerResponse containerInfo = mock(InspectContainerResponse.class);
        NetworkSettings networkSettings = mock(NetworkSettings.class);
        Ports ports = mock(Ports.class);

        // Set up the mock port bindings for TCP port 8080
        Map<ExposedPort, Ports.Binding[]> bindings = new HashMap<>();
        ExposedPort tcpPort = new ExposedPort(8080, com.github.dockerjava.api.model.InternetProtocol.TCP);
        bindings.put(tcpPort, new Ports.Binding[] { Ports.Binding.bindPort(54321) });

        when(containerState.getContainerId()).thenReturn("test-container-id");
        when(containerState.getContainerInfo()).thenReturn(containerInfo);
        when(containerInfo.getNetworkSettings()).thenReturn(networkSettings);
        when(networkSettings.getPorts()).thenReturn(ports);
        when(ports.getBindings()).thenReturn(bindings);
        doCallRealMethod().when(containerState).getMappedPort(8080, InternetProtocol.TCP);
        doCallRealMethod().when(containerState).getMappedPort(8080);

        // Test with explicit TCP protocol
        Integer mappedPort = containerState.getMappedPort(8080, InternetProtocol.TCP);
        assertThat(mappedPort).isEqualTo(54321);

        // Test default getMappedPort (should also return same value since it defaults to TCP)
        Integer defaultMappedPort = containerState.getMappedPort(8080);
        assertThat(defaultMappedPort).isEqualTo(54321);
    }

    @Test
    void shouldThrowForUnmappedUdpPort() {
        ContainerState containerState = mock(ContainerState.class);
        InspectContainerResponse containerInfo = mock(InspectContainerResponse.class);
        NetworkSettings networkSettings = mock(NetworkSettings.class);
        Ports ports = mock(Ports.class);

        // Set up the mock port bindings with only TCP port
        Map<ExposedPort, Ports.Binding[]> bindings = new HashMap<>();
        ExposedPort tcpPort = new ExposedPort(8080, com.github.dockerjava.api.model.InternetProtocol.TCP);
        bindings.put(tcpPort, new Ports.Binding[] { Ports.Binding.bindPort(54321) });

        when(containerState.getContainerId()).thenReturn("test-container-id");
        when(containerState.getContainerInfo()).thenReturn(containerInfo);
        when(containerInfo.getNetworkSettings()).thenReturn(networkSettings);
        when(networkSettings.getPorts()).thenReturn(ports);
        when(ports.getBindings()).thenReturn(bindings);
        doCallRealMethod().when(containerState).getMappedPort(8080, InternetProtocol.UDP);

        // Should throw when trying to get unmapped UDP port
        assertThatThrownBy(() -> containerState.getMappedPort(8080, InternetProtocol.UDP))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("8080/udp")
            .hasMessageContaining("is not mapped");
    }

    @Test
    void shouldSupportBothTcpAndUdpOnSamePort() {
        ContainerState containerState = mock(ContainerState.class);
        InspectContainerResponse containerInfo = mock(InspectContainerResponse.class);
        NetworkSettings networkSettings = mock(NetworkSettings.class);
        Ports ports = mock(Ports.class);

        // Set up the mock port bindings with both TCP and UDP on the same port
        Map<ExposedPort, Ports.Binding[]> bindings = new HashMap<>();
        ExposedPort tcpPort = new ExposedPort(5000, com.github.dockerjava.api.model.InternetProtocol.TCP);
        ExposedPort udpPort = new ExposedPort(5000, com.github.dockerjava.api.model.InternetProtocol.UDP);
        bindings.put(tcpPort, new Ports.Binding[] { Ports.Binding.bindPort(11111) });
        bindings.put(udpPort, new Ports.Binding[] { Ports.Binding.bindPort(22222) });

        when(containerState.getContainerId()).thenReturn("test-container-id");
        when(containerState.getContainerInfo()).thenReturn(containerInfo);
        when(containerInfo.getNetworkSettings()).thenReturn(networkSettings);
        when(networkSettings.getPorts()).thenReturn(ports);
        when(ports.getBindings()).thenReturn(bindings);
        doCallRealMethod().when(containerState).getMappedPort(5000, InternetProtocol.TCP);
        doCallRealMethod().when(containerState).getMappedPort(5000, InternetProtocol.UDP);

        // Both should be mapped to different host ports
        Integer tcpMappedPort = containerState.getMappedPort(5000, InternetProtocol.TCP);
        Integer udpMappedPort = containerState.getMappedPort(5000, InternetProtocol.UDP);

        assertThat(tcpMappedPort).isEqualTo(11111);
        assertThat(udpMappedPort).isEqualTo(22222);
    }
}

package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.InternetProtocol;
import com.github.dockerjava.api.model.NetworkSettings;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;

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
    void getMappedPortWithTcpProtocolReturnsMappedPort() {
        ContainerState containerState = mock(ContainerState.class);
        doCallRealMethod().when(containerState).getMappedPort(8080);
        doCallRealMethod().when(containerState).getMappedPort(8080, InternetProtocol.TCP);
        when(containerState.getContainerId()).thenReturn("test-container-id");

        Ports ports = new Ports();
        ports.bind(new ExposedPort(8080, InternetProtocol.TCP), Ports.Binding.bindPort(32768));

        NetworkSettings networkSettings = mock(NetworkSettings.class);
        when(networkSettings.getPorts()).thenReturn(ports);

        InspectContainerResponse containerInfo = mock(InspectContainerResponse.class);
        when(containerInfo.getNetworkSettings()).thenReturn(networkSettings);
        when(containerState.getContainerInfo()).thenReturn(containerInfo);

        assertThat(containerState.getMappedPort(8080)).isEqualTo(32768);
        assertThat(containerState.getMappedPort(8080, InternetProtocol.TCP)).isEqualTo(32768);
    }

    @Test
    void getMappedPortWithUdpProtocolReturnsMappedPort() {
        ContainerState containerState = mock(ContainerState.class);
        doCallRealMethod().when(containerState).getMappedPort(53, InternetProtocol.UDP);
        when(containerState.getContainerId()).thenReturn("test-container-id");

        Ports ports = new Ports();
        ports.bind(new ExposedPort(53, InternetProtocol.UDP), Ports.Binding.bindPort(32769));

        NetworkSettings networkSettings = mock(NetworkSettings.class);
        when(networkSettings.getPorts()).thenReturn(ports);

        InspectContainerResponse containerInfo = mock(InspectContainerResponse.class);
        when(containerInfo.getNetworkSettings()).thenReturn(networkSettings);
        when(containerState.getContainerInfo()).thenReturn(containerInfo);

        assertThat(containerState.getMappedPort(53, InternetProtocol.UDP)).isEqualTo(32769);
    }

    @Test
    void getMappedPortThrowsWhenPortNotMapped() {
        ContainerState containerState = mock(ContainerState.class);
        doCallRealMethod().when(containerState).getMappedPort(9999, InternetProtocol.UDP);
        when(containerState.getContainerId()).thenReturn("test-container-id");

        Ports ports = new Ports();

        NetworkSettings networkSettings = mock(NetworkSettings.class);
        when(networkSettings.getPorts()).thenReturn(ports);

        InspectContainerResponse containerInfo = mock(InspectContainerResponse.class);
        when(containerInfo.getNetworkSettings()).thenReturn(networkSettings);
        when(containerState.getContainerInfo()).thenReturn(containerInfo);

        assertThatThrownBy(() -> containerState.getMappedPort(9999, InternetProtocol.UDP))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("9999/udp");
    }
}

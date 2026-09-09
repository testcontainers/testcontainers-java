package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
    void getMappedPortDistinguishesTcpAndUdpBindingsForSamePort() {
        ContainerState containerState = mock(ContainerState.class);
        doCallRealMethod().when(containerState).getMappedPort(anyInt());
        doCallRealMethod().when(containerState).getMappedPort(anyInt(), any());
        when(containerState.getContainerId()).thenReturn("container-id");

        InspectContainerResponse containerInfo = mock(InspectContainerResponse.class, Answers.RETURNS_DEEP_STUBS);
        Map<ExposedPort, Ports.Binding[]> bindings = new HashMap<>();
        bindings.put(ExposedPort.tcp(8080), new Ports.Binding[] { Ports.Binding.bindPort(12345) });
        bindings.put(ExposedPort.udp(8080), new Ports.Binding[] { Ports.Binding.bindPort(54321) });
        when(containerInfo.getNetworkSettings().getPorts().getBindings()).thenReturn(bindings);
        when(containerState.getContainerInfo()).thenReturn(containerInfo);

        assertThat(containerState.getMappedPort(8080)).isEqualTo(12345);
        assertThat(containerState.getMappedPort(8080, InternetProtocol.TCP)).isEqualTo(12345);
        assertThat(containerState.getMappedPort(8080, InternetProtocol.UDP)).isEqualTo(54321);
    }

    @Test
    void getMappedPortThrowsWhenPortNotMappedForProtocol() {
        ContainerState containerState = mock(ContainerState.class);
        doCallRealMethod().when(containerState).getMappedPort(anyInt(), any());
        when(containerState.getContainerId()).thenReturn("container-id");

        InspectContainerResponse containerInfo = mock(InspectContainerResponse.class, Answers.RETURNS_DEEP_STUBS);
        Map<ExposedPort, Ports.Binding[]> bindings = new HashMap<>();
        bindings.put(ExposedPort.tcp(8080), new Ports.Binding[] { Ports.Binding.bindPort(12345) });
        when(containerInfo.getNetworkSettings().getPorts().getBindings()).thenReturn(bindings);
        when(containerState.getContainerInfo()).thenReturn(containerInfo);

        assertThatThrownBy(() -> containerState.getMappedPort(8080, InternetProtocol.UDP))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("udp");
    }
}

package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void getMappedPortWithProtocolLooksUpTheBindingForThatProtocol() {
        ContainerState containerState = mock(ContainerState.class);
        doCallRealMethod().when(containerState).getMappedPort(anyInt(), any());
        when(containerState.getContainerId()).thenReturn("container-id");

        InspectContainerResponse containerInfo = Mockito.mock(InspectContainerResponse.class, Answers.RETURNS_DEEP_STUBS);
        ExposedPort udpPort = new ExposedPort(12345, com.github.dockerjava.api.model.InternetProtocol.UDP);
        when(containerInfo.getNetworkSettings().getPorts().getBindings())
            .thenReturn(Collections.singletonMap(udpPort, new Ports.Binding[] { Ports.Binding.bindPort(54321) }));
        when(containerState.getContainerInfo()).thenReturn(containerInfo);

        Integer mappedPort = containerState.getMappedPort(12345, InternetProtocol.UDP);

        assertThat(mappedPort).isEqualTo(54321);
    }

    @Test
    void getMappedPortWithProtocolThrowsWhenNotMapped() {
        ContainerState containerState = mock(ContainerState.class);
        doCallRealMethod().when(containerState).getMappedPort(anyInt(), any());
        when(containerState.getContainerId()).thenReturn("container-id");

        InspectContainerResponse containerInfo = Mockito.mock(InspectContainerResponse.class, Answers.RETURNS_DEEP_STUBS);
        when(containerInfo.getNetworkSettings().getPorts().getBindings()).thenReturn(Collections.emptyMap());
        when(containerState.getContainerInfo()).thenReturn(containerInfo);

        org.assertj.core.api.Assertions
            .assertThatThrownBy(() -> containerState.getMappedPort(12345, InternetProtocol.UDP))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("12345/udp");
    }
}

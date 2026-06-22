package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.NetworkSettings;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
    void getMappedPortUsesCurrentContainerInfo() {
        ContainerState containerState = mock(ContainerState.class);
        doCallRealMethod().when(containerState).getMappedPort(8080);
        InspectContainerResponse cachedInfo = inspectResponse(8080, 18080);
        InspectContainerResponse currentInfo = inspectResponse(8080, 28080);

        when(containerState.getContainerId()).thenReturn("container-id");
        when(containerState.getContainerInfo()).thenReturn(cachedInfo);
        when(containerState.getCurrentContainerInfo()).thenReturn(currentInfo);

        assertThat(containerState.getMappedPort(8080)).isEqualTo(28080);
    }

    @Test
    void getFirstMappedPortUsesCurrentContainerInfo() {
        ContainerState containerState = mock(ContainerState.class);
        doCallRealMethod().when(containerState).getMappedPort(8080);
        doCallRealMethod().when(containerState).getFirstMappedPort();
        InspectContainerResponse cachedInfo = inspectResponse(8080, 18080);
        InspectContainerResponse currentInfo = inspectResponse(8080, 28080);

        when(containerState.getContainerId()).thenReturn("container-id");
        when(containerState.getExposedPorts()).thenReturn(Collections.singletonList(8080));
        when(containerState.getContainerInfo()).thenReturn(cachedInfo);
        when(containerState.getCurrentContainerInfo()).thenReturn(currentInfo);

        assertThat(containerState.getFirstMappedPort()).isEqualTo(28080);
    }

    @Test
    void getPortBindingsUsesCurrentContainerInfo() {
        ContainerState containerState = mock(ContainerState.class);
        doCallRealMethod().when(containerState).getPortBindings();
        InspectContainerResponse cachedInfo = inspectResponse(8080, 18080);
        InspectContainerResponse currentInfo = inspectResponse(8080, 28080);

        when(containerState.getContainerInfo()).thenReturn(cachedInfo);
        when(containerState.getCurrentContainerInfo()).thenReturn(currentInfo);

        assertThat(containerState.getPortBindings()).containsExactly("28080:8080/tcp");
    }

    private static InspectContainerResponse inspectResponse(int containerPort, int hostPort) {
        InspectContainerResponse response = mock(InspectContainerResponse.class);
        NetworkSettings networkSettings = mock(NetworkSettings.class);
        HostConfig hostConfig = mock(HostConfig.class);
        Ports ports = new Ports();
        ExposedPort exposedPort = new ExposedPort(containerPort);
        Ports.Binding binding = Ports.Binding.bindPort(hostPort);
        ports.bind(exposedPort, binding);

        when(response.getNetworkSettings()).thenReturn(networkSettings);
        when(networkSettings.getPorts()).thenReturn(ports);
        when(response.getHostConfig()).thenReturn(hostConfig);
        when(hostConfig.getPortBindings()).thenReturn(ports);

        return response;
    }
}

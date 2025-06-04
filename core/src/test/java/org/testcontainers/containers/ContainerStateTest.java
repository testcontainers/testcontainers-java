package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ParameterizedClass(name = "{0} ({1} -> {2})")
@MethodSource("params")
public class ContainerStateTest {

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

    @Parameter(0)
    public String name;

    @Parameter(1)
    public String testSet;

    @Parameter(2)
    public List<Integer> expectedResult;

    @Test
    public void test() {
        ContainerState containerState = mock(ContainerState.class);
        doCallRealMethod().when(containerState).getBoundPortNumbers();

        when(containerState.getPortBindings()).thenReturn(Collections.singletonList(testSet));

        List<Integer> result = containerState.getBoundPortNumbers();
        assertThat(result).hasSameElementsAs(expectedResult);
    }
}

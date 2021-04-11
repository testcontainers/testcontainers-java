package org.testcontainers.containers;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class ContainerStateTest {

    @Parameterized.Parameters(name = "{0} ({1} -> {2})")
    public static Object[][] params() {
        return new Object[][]{
            new Object[]{"regular mapping", "80:8080/tcp", singletonList(80),},
            new Object[]{"regular mapping with host", "127.0.0.1:80:8080/tcp", singletonList(80),},
            new Object[]{"zero port without host", ":0:8080/tcp", emptyList(),},
            new Object[]{"missing port with host", "0.0.0.0:0:8080/tcp", emptyList(),},
            new Object[]{"zero port (synthetic case)", "0:8080/tcp", emptyList(),},
            new Object[]{"missing port", ":8080/tcp", emptyList(),
            }
        };
    }

    @Parameterized.Parameter(0)
    public String name;
    @Parameterized.Parameter(1)
    public String testSet;
    @Parameterized.Parameter(2)
    public List<Integer> expectedResult;

    @Test
    public void test() {
        ContainerState containerState = mock(ContainerState.class);
        doCallRealMethod().when(containerState).getBoundPortNumbers();

        when(containerState.getPortBindings()).thenReturn(singletonList(testSet));

        List<Integer> result = containerState.getBoundPortNumbers();
        Assertions.assertThat(result).hasSameElementsAs(expectedResult);
    }
}

package org.testcontainers.containers;

import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;
import static org.rnorth.visibleassertions.VisibleAssertions.*;

public class ContainerStateTest {

    @Mock
    ContainerState containerState;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        doCallRealMethod().when(containerState).getBoundPortNumbers();
    }

    void testSet(String... args) {
        when(containerState.getPortBindings()).thenReturn(
            Lists.list(args)
        );
    }

    @Test
    public void testGetBoundPortNumbers() {
        testSet("80:8080/tcp");
        List<Integer> res = containerState.getBoundPortNumbers();
        assertEquals(null,1,res.size());
        assertEquals("regular mapping works",80, res.stream().findFirst().get());
    }

    @Test
    public void testGetBoundPortNumbersFull() {
        testSet("127.0.0.1:80:8080/tcp");
        List<Integer> res = containerState.getBoundPortNumbers();
        assertEquals(null,1,res.size());
        assertEquals("regular mapping with host works",80, res.stream().findFirst().get());
    }

    @Test
    public void testGetBoundPortNumbersFullExplicitZero() {
        testSet(":0:8080/tcp");
        List<Integer> res = containerState.getBoundPortNumbers();
        assertEquals("zero port with host is ignored",0,res.size());
    }

    @Test
    public void testGetBoundPortNumbersFullImplicitZero() {
        testSet("0.0.0.0::8080/tcp");
        List<Integer> res = containerState.getBoundPortNumbers();
        assertEquals("missing port with host is ignored",0,res.size());
    }

    @Test
    public void testGetBoundPortNumbersExplicitZero() {
        testSet("0:8080/tcp");
        List<Integer> res = containerState.getBoundPortNumbers();
        assertEquals("zero port (synthetic case) is ignored",0,res.size());
    }

    @Test
    public void testGetBoundPortNumbersImplicitZero() {
        testSet(":8080/tcp");
        List<Integer> res = containerState.getBoundPortNumbers();
        assertEquals("missing port is ignored",0,res.size());
    }
}

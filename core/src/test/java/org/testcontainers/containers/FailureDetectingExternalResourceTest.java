package org.testcontainers.containers;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class FailureDetectingExternalResourceTest {

    @Test
    public void finishedIsCalledForCleanupIfStartingThrows() throws Throwable {
        FailureDetectingExternalResource res = spy(FailureDetectingExternalResource.class);
        Statement stmt = res.apply(mock(Statement.class), Description.EMPTY);
        doThrow(new RuntimeException()).when(res).starting(any());
        try {
            stmt.evaluate();
        } catch (Throwable t) {
            // ignore
        }
        verify(res).starting(any());
        verify(res).finished(any());
    }

}

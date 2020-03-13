package org.testcontainers.containers;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Has an overhead of connecting to Docker and running Ryuk
 * only once thanks to a static class member variable MONGODB_CONTAINER.
 *
 * @author Konstantin Silaev on 9/30/2019
 */
@RunWith(MockitoJUnitRunner.class)
public class MongoDbContainerTest {
    private static final int CONTAINER_ERROR_EXIT_CODE = 252;
    @Spy
    private static MongoDbContainer MONGODB_CONTAINER = new MongoDbContainer();

    @Test(expected = IllegalStateException.class)
    public void shouldNotGetReplicaSetUrlBecauseOfNotStartedContainer() {
        //WHEN
        MONGODB_CONTAINER.getReplicaSetUrl();

        //THEN
        //expected IllegalStateException.class
    }

    @Test(expected = MongoDbContainer.ReplicaSetInitializationException.class)
    public void shouldNotInitReplicaSetBecauseOfExecInitReplicaSet()
        throws IOException, InterruptedException {
        //GIVEN
        Container.ExecResult mockExecResult = mock(Container.ExecResult.class);
        when(mockExecResult.getExitCode())
            .thenReturn(CONTAINER_ERROR_EXIT_CODE);
        doReturn(mockExecResult).when(MONGODB_CONTAINER).execInContainer(any());
        final int mappedPort = 37723;
        lenient().doReturn(mappedPort).when(MONGODB_CONTAINER)
            .getMappedPort(MongoDbContainer.MONGODB_INTERNAL_PORT);

        //WHEN
        MONGODB_CONTAINER.initReplicaSet();

        //THEN
        //expected = MongoDbContainer.ReplicaSetInitializationException.class
    }

    @Test(expected = MongoDbContainer.ReplicaSetInitializationException.class)
    public void shouldNotInitReplicaSetBecauseOfWaitCommand() throws IOException, InterruptedException {
        //GIVEN
        final Container.ExecResult mockExecResult1 = mock(Container.ExecResult.class);
        final Container.ExecResult mockExecResult2 = mock(Container.ExecResult.class);
        when(mockExecResult1.getExitCode())
            .thenReturn(0);
        when(mockExecResult2.getExitCode())
            .thenReturn(252);
        doReturn(mockExecResult1, mockExecResult2)
            .when(MONGODB_CONTAINER).execInContainer(any());
        final int mappedPort = 37723;
        lenient().doReturn(mappedPort).when(MONGODB_CONTAINER)
            .getMappedPort(MongoDbContainer.MONGODB_INTERNAL_PORT);

        //WHEN
        MONGODB_CONTAINER.initReplicaSet();

        //THEN
        //expected = MongoDbContainer.ReplicaSetInitializationException.class
    }
}

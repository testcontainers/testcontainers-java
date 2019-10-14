package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
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
@ExtendWith(MockitoExtension.class)
class MongoDbContainerTest {
    @Spy
    private static MongoDbContainer MONGODB_CONTAINER = new MongoDbContainer();

    @Test
    void shouldNotGetReplicaSetUrlBecauseOfNotStartedContainer() {
        //WHEN
        Executable executable = () -> MONGODB_CONTAINER.getReplicaSetUrl();

        //THEN
        assertThrows(IllegalStateException.class, executable);
    }

    @Test
    void shouldNotInitReplicaSetBecauseOfExecInitReplicaSet()
        throws IOException, InterruptedException {
        //GIVEN
        Container.ExecResult mockExecResult = mock(Container.ExecResult.class);
        when(mockExecResult.getExitCode())
            .thenReturn(MongoDbContainer.ERROR_CONTAINER_EXIT_CODE);
        doReturn(mockExecResult).when(MONGODB_CONTAINER).execInContainer(any());
        final int mappedPort = 37723;
        lenient().doReturn(mappedPort).when(MONGODB_CONTAINER)
            .getMappedPort(MongoDbContainer.MONGODB_INTERNAL_PORT);

        //WHEN
        Executable executable = () -> MONGODB_CONTAINER.initReplicaSet();

        //THEN
        assertThrows(
            MongoDbContainer.ReplicaSetInitializationException.class,
            executable
        );
    }

    @Test
    void shouldNotInitReplicaSetBecauseOfWaitCommand() throws IOException, InterruptedException {
        //GIVEN
        final Container.ExecResult mockExecResult1 = mock(Container.ExecResult.class);
        final Container.ExecResult mockExecResult2 = mock(Container.ExecResult.class);
        when(mockExecResult1.getExitCode())
            .thenReturn(0);
        when(mockExecResult2.getExitCode())
            .thenReturn(MongoDbContainer.ERROR_CONTAINER_EXIT_CODE);
        doReturn(mockExecResult1, mockExecResult2)
            .when(MONGODB_CONTAINER).execInContainer(any());
        final int mappedPort = 37723;
        lenient().doReturn(mappedPort).when(MONGODB_CONTAINER)
            .getMappedPort(MongoDbContainer.MONGODB_INTERNAL_PORT);

        //WHEN
        Executable executable = () -> MONGODB_CONTAINER.initReplicaSet();

        //THEN
        assertThrows(
            MongoDbContainer.ReplicaSetInitializationException.class,
            executable
        );
    }
}

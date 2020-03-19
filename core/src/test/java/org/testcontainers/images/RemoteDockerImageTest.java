package org.testcontainers.images;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.Mockito;
import org.testcontainers.utility.Base58;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class RemoteDockerImageTest {

    @Test
    public void toStringContainsOnlyImageName() {
        String imageName = Base58.randomString(8).toLowerCase();
        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(imageName);
        assertThat(remoteDockerImage.toString(), containsString(imageName));
        assertThat(remoteDockerImage.toString(), not(containsString("imageNameFuture")));
    }

    @Test
    public void toStringWithExceptionContainsOnlyImageNameFuture() throws Exception {
        Future<String> imageNameFuture = Mockito.mock(Future.class);
        when(imageNameFuture.get()).thenThrow(new ExecutionException(new RuntimeException("arbitrary")));
        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(imageNameFuture);
        assertThat(remoteDockerImage.toString(), containsString("imageNameFuture"));
    }
}

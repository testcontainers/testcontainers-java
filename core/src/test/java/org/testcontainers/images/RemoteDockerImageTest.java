package org.testcontainers.images;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.testcontainers.utility.Base58;

import java.util.concurrent.CompletableFuture;

public class RemoteDockerImageTest {

    @Test
    public void toStringContainsOnlyImageName() {
        String imageName = Base58.randomString(8).toLowerCase();
        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(imageName);
        String toString = remoteDockerImage.toString();
        assertThat(toString, containsString("imageName=" + imageName));
        assertThat(toString, not(containsString("imageNameFuture=")));
    }

    @Test
    public void toStringWithExceptionContainsOnlyImageNameFuture()  {
        CompletableFuture<String> imageNameFuture = new CompletableFuture<>();
        imageNameFuture.completeExceptionally(new RuntimeException("arbitrary"));
        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(imageNameFuture);
        String toString = remoteDockerImage.toString();
        assertThat(toString, containsString("imageNameFuture="));
        assertThat(toString, not(containsString("imageName=")));
    }
}

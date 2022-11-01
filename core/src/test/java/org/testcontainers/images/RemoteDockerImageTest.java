package org.testcontainers.images;

import org.junit.Test;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.LazyFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

public class RemoteDockerImageTest {

    @Test
    public void toStringContainsOnlyImageName() {
        String imageName = Base58.randomString(8).toLowerCase();
        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(DockerImageName.parse(imageName));
        assertThat(remoteDockerImage.toString()).contains("imageName=" + imageName);
    }

    @Test
    public void toStringWithExceptionContainsOnlyImageNameFuture() {
        CompletableFuture<String> imageNameFuture = new CompletableFuture<>();
        imageNameFuture.completeExceptionally(new RuntimeException("arbitrary"));

        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(imageNameFuture);
        assertThat(remoteDockerImage.toString()).contains("imageName=java.lang.RuntimeException: arbitrary");
    }

    @Test(timeout = 5000L)
    public void toStringDoesntResolveImageNameFuture() {
        CompletableFuture<String> imageNameFuture = new CompletableFuture<>();

        // verify that we've set up the test properly
        assertThat(imageNameFuture).isNotDone();

        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(imageNameFuture);
        assertThat(remoteDockerImage.toString()).contains("imageName=<resolving>");

        // Make sure the act of calling toString doesn't resolve the imageNameFuture
        assertThat(imageNameFuture).isNotDone();

        String imageName = Base58.randomString(8).toLowerCase();
        imageNameFuture.complete(imageName);
        assertThat(remoteDockerImage.toString()).contains("imageName=" + imageName);
    }

    @Test(timeout = 5000L)
    public void toStringDoesntResolveLazyFuture() throws Exception {
        String imageName = Base58.randomString(8).toLowerCase();
        AtomicBoolean resolved = new AtomicBoolean(false);
        Future<String> imageNameFuture = new LazyFuture<String>() {
            @Override
            protected String resolve() {
                resolved.set(true);
                return imageName;
            }
        };

        // verify that we've set up the test properly
        assertThat(imageNameFuture).isNotDone();

        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(imageNameFuture);
        assertThat(remoteDockerImage.toString()).contains("imageName=<resolving>");

        // Make sure the act of calling toString doesn't resolve the imageNameFuture
        assertThat(imageNameFuture).isNotDone();
        assertThat(resolved).isFalse();

        // Trigger resolve
        imageNameFuture.get();
        assertThat(remoteDockerImage.toString()).contains("imageName=" + imageName);
    }
}

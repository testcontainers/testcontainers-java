package org.testcontainers.images;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.PullImageCmd;
import org.testcontainers.images.TimeLimitedLoggedPullImageResultCallback;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.LazyFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteDockerImageTest {

    @Test
    void toStringContainsOnlyImageName() {
        String imageName = Base58.randomString(8).toLowerCase();
        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(DockerImageName.parse(imageName));
        assertThat(remoteDockerImage.toString()).contains("imageName=" + imageName);
    }

    @Test
    void toStringWithExceptionContainsOnlyImageNameFuture() {
        CompletableFuture<String> imageNameFuture = new CompletableFuture<>();
        imageNameFuture.completeExceptionally(new RuntimeException("arbitrary"));

        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(imageNameFuture);
        assertThat(remoteDockerImage.toString()).contains("imageName=java.lang.RuntimeException: arbitrary");
    }

    @Test
    @Timeout(5)
    void toStringDoesntResolveImageNameFuture() {
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

    @Test
    @Timeout(5)
    void toStringDoesntResolveLazyFuture() throws Exception {
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
    @Test
    void passesExplicitPlatformToPullImageCommand() throws Exception {
        DockerClient dockerClient = mock(DockerClient.class);
        PullImageCmd pullImageCmd = mock(PullImageCmd.class);

        when(dockerClient.pullImageCmd("test/image")).thenReturn(pullImageCmd);
        when(pullImageCmd.withTag("latest")).thenReturn(pullImageCmd);
        when(pullImageCmd.withPlatform("linux/amd64")).thenReturn(pullImageCmd);
        TimeLimitedLoggedPullImageResultCallback callback = mock(TimeLimitedLoggedPullImageResultCallback.class);
        when(pullImageCmd.exec(any(TimeLimitedLoggedPullImageResultCallback.class))).thenReturn(callback);
        when(callback.awaitCompletion()).thenReturn(callback);

        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(DockerImageName.parse("test/image:latest"))
            .withImagePlatform("linux/amd64");

        remoteDockerImage.withDockerClient(dockerClient).get();

        verify(pullImageCmd).withPlatform("linux/amd64");
    }
    
}

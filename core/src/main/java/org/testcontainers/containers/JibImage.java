package org.testcontainers.containers;

import com.google.cloud.tools.jib.api.CacheDirectoryCreationException;
import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerClient;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.InvalidImageReferenceException;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import com.google.cloud.tools.jib.api.RegistryException;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.LazyFuture;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

public class JibImage extends LazyFuture<String> {

    private final DockerClient dockerClient = JibDockerClient.instance();

    private final String image;

    private final Function<JibContainerBuilder, JibContainerBuilder> jibContainerBuilderFn;

    public JibImage(String image, Function<JibContainerBuilder, JibContainerBuilder> jibContainerBuilderFn) {
        this.image = image;
        this.jibContainerBuilderFn = jibContainerBuilderFn;
    }

    @Override
    protected String resolve() {
        try {
            JibContainerBuilder containerBuilder = Jib.from(this.dockerClient, DockerDaemonImage.named(this.image));
            JibContainer jibContainer =
                this.jibContainerBuilderFn.apply(containerBuilder)
                    .containerize(
                        Containerizer.to(
                            this.dockerClient,
                            DockerDaemonImage.named(Base58.randomString(8).toLowerCase())
                        )
                    );
            return jibContainer.getTargetImage().toString();
        } catch (
            InvalidImageReferenceException
            | CacheDirectoryCreationException
            | IOException
            | ExecutionException
            | InterruptedException
            | RegistryException e
        ) {
            throw new RuntimeException(e);
        }
    }
}

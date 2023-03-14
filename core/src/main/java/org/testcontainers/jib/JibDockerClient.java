package org.testcontainers.jib;

import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.LoadImageCallback;
import com.google.cloud.tools.jib.api.DockerClient;
import com.google.cloud.tools.jib.api.ImageDetails;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.http.NotifyingOutputStream;
import com.google.cloud.tools.jib.image.ImageTarball;
import com.google.common.io.ByteStreams;
import lombok.Cleanup;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.UnstableAPI;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

@UnstableAPI
class JibDockerClient implements DockerClient {

    private static JibDockerClient instance;

    private final com.github.dockerjava.api.DockerClient dockerClient = DockerClientFactory.lazyClient();

    public static JibDockerClient instance() {
        if (instance == null) {
            instance = new JibDockerClient();
        }

        return instance;
    }

    @Override
    public boolean supported(Map<String, String> map) {
        return false;
    }

    @Override
    public String load(ImageTarball imageTarball, Consumer<Long> writtenByteCountListener) throws IOException {
        @Cleanup
        PipedInputStream in = new PipedInputStream();
        @Cleanup
        PipedOutputStream out = new PipedOutputStream(in);
        LoadImageCallback loadImage = this.dockerClient.loadImageAsyncCmd(in).exec(new LoadImageCallback());

        try (NotifyingOutputStream stdin = new NotifyingOutputStream(out, writtenByteCountListener)) {
            imageTarball.writeTo(stdin);
        }

        return loadImage.awaitMessage();
    }

    @Override
    public void save(ImageReference imageReference, Path outputPath, Consumer<Long> writtenByteCountListener)
        throws IOException {
        try (
            InputStream inputStream = this.dockerClient.saveImageCmd(imageReference.toString()).exec();
            InputStream stdout = new BufferedInputStream(inputStream);
            OutputStream fileStream = new BufferedOutputStream(Files.newOutputStream(outputPath));
            NotifyingOutputStream notifyingFileStream = new NotifyingOutputStream(fileStream, writtenByteCountListener)
        ) {
            ByteStreams.copy(stdout, notifyingFileStream);
        }
    }

    @Override
    public ImageDetails inspect(ImageReference imageReference) {
        new RemoteDockerImage(DockerImageName.parse(imageReference.toString())).get();

        InspectImageResponse response = this.dockerClient.inspectImageCmd(imageReference.toString()).exec();
        return new JibImageDetails(response.getSize(), response.getId(), response.getRootFS().getLayers());
    }
}

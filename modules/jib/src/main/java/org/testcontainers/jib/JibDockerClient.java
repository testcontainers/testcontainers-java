package org.testcontainers.jib;

import com.github.dockerjava.api.command.InspectImageResponse;
import com.github.dockerjava.api.command.PullImageCmd;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.google.cloud.tools.jib.api.DockerClient;
import com.google.cloud.tools.jib.api.ImageDetails;
import com.google.cloud.tools.jib.api.ImageReference;
import com.google.cloud.tools.jib.http.NotifyingOutputStream;
import com.google.cloud.tools.jib.image.ImageTarball;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import org.testcontainers.DockerClientFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;

public class JibDockerClient implements DockerClient {

    com.github.dockerjava.api.DockerClient dockerClient = DockerClientFactory.lazyClient();

    @Override
    public boolean supported(Map<String, String> map) {
        return false;
    }

    @Override
    public String load(ImageTarball imageTarball, Consumer<Long> writtenByteCountListener) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        imageTarball.writeTo(out);
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        this.dockerClient.loadImageCmd(in).exec();
        try (InputStreamReader stdout = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return CharStreams.toString(stdout);
        }
    }

    @Override
    public void save(ImageReference imageReference, Path outputPath, Consumer<Long> writtenByteCountListener)
        throws IOException {
        InputStream inputStream =
            this.dockerClient.saveImageCmd(imageReference + ":" + imageReference.getTag().get()).exec();
        try (
            InputStream stdout = new BufferedInputStream(inputStream);
            OutputStream fileStream = new BufferedOutputStream(Files.newOutputStream(outputPath));
            NotifyingOutputStream notifyingFileStream = new NotifyingOutputStream(fileStream, writtenByteCountListener)
        ) {
            ByteStreams.copy(stdout, notifyingFileStream);
        }
    }

    @Override
    public ImageDetails inspect(ImageReference imageReference) throws InterruptedException {
        PullImageCmd pullImageCmd =
            this.dockerClient.pullImageCmd(imageReference.getRepository())
                .withRegistry(imageReference.getRegistry())
                .withTag(imageReference.getTag().get());
        pullImageCmd.exec(new PullImageResultCallback()).awaitCompletion();
        InspectImageResponse response =
            this.dockerClient.inspectImageCmd(imageReference.getRepository() + ":" + imageReference.getTag().get())
                .exec();
        return new JibImageDetails(response.getSize(), response.getId(), response.getRootFS().getLayers());
    }
}

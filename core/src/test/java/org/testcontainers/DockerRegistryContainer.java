package org.testcontainers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DockerRegistryContainer extends GenericContainer<DockerRegistryContainer> {

    @Getter
    String endpoint;

    public DockerRegistryContainer() {
        super(TestImages.DOCKER_REGISTRY_IMAGE);
    }

    public DockerRegistryContainer(@NonNull Future<String> image) {
        super(image);
    }

    @Override
    protected void configure() {
        super.configure();
        withEnv("REGISTRY_HTTP_ADDR", "127.0.0.1:0");
        withCreateContainerCmdModifier(cmd -> {
            cmd.getHostConfig().withNetworkMode("host");
        });
    }

    @Override
    @SneakyThrows
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        AtomicInteger port = new AtomicInteger(-1);
        try (FrameConsumerResultCallback resultCallback = new FrameConsumerResultCallback()) {
            WaitingConsumer waitingConsumer = new WaitingConsumer();
            resultCallback.addConsumer(OutputFrame.OutputType.STDERR, waitingConsumer);

            dockerClient
                .logContainerCmd(containerInfo.getId())
                .withStdErr(true)
                .withFollowStream(true)
                .exec(resultCallback);

            Pattern pattern = Pattern.compile(
                ".*listening on .*:(\\d+).*",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
            );
            waitingConsumer.waitUntil(
                it -> {
                    String s = it.getUtf8String();
                    Matcher matcher = pattern.matcher(s);
                    if (matcher.matches()) {
                        port.set(Integer.parseInt(matcher.group(1)));
                        return true;
                    } else {
                        return false;
                    }
                },
                10,
                TimeUnit.SECONDS
            );
        }

        endpoint = getHost() + ":" + port.get();
    }

    public DockerImageName createImage() {
        return createImage(UUID.randomUUID().toString());
    }

    public DockerImageName createImage(String tag) {
        return createImage("testcontainers/helloworld:latest", tag);
    }

    @SneakyThrows(InterruptedException.class)
    public DockerImageName createImage(String originalImage, String tag) {
        DockerClient client = getDockerClient();
        client.pullImageCmd(originalImage).exec(new PullImageResultCallback()).awaitCompletion();

        String dummyImageId = client.inspectImageCmd(originalImage).exec().getId();

        DockerImageName imageName = DockerImageName
            .parse(getEndpoint() + "/" + Base58.randomString(6).toLowerCase())
            .withTag(tag);

        // push the image to the registry
        client.tagImageCmd(dummyImageId, imageName.asCanonicalNameString(), tag).exec();

        client
            .pushImageCmd(imageName.asCanonicalNameString())
            .exec(new ResultCallback.Adapter<>())
            .awaitCompletion(1, TimeUnit.MINUTES);

        // Remove from local cache, tests should pull the image themselves
        client.removeImageCmd(imageName.asCanonicalNameString()).exec();

        return imageName;
    }
}

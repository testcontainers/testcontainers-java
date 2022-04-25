package org.testcontainers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.WaitingConsumer;

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

            dockerClient.logContainerCmd(containerInfo.getId())
                .withStdErr(true)
                .withFollowStream(true)
                .exec(resultCallback);

            Pattern pattern = Pattern.compile(".*listening on .*:(\\d+).*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            waitingConsumer.waitUntil(it -> {
                String s = it.getUtf8String();
                Matcher matcher = pattern.matcher(s);
                if (matcher.matches()) {
                    port.set(Integer.parseInt(matcher.group(1)));
                    return true;
                } else {
                    return false;
                }
            }, 10, TimeUnit.SECONDS);
        }

        endpoint = getHost() + ":" + port.get();
    }
}

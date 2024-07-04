package org.testcontainers.nvidia;

import com.github.dockerjava.api.model.DeviceRequest;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.RuntimeInfo;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

public class NimContainer extends GenericContainer<NimContainer> {

    //    private static final DockerImageName DOCKER_IMAGE_NAME = DockerImageName.parse("ollama/ollama");

    public NimContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public NimContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        //        dockerImageName.assertCompatibleWith(DOCKER_IMAGE_NAME);

        Info info = this.dockerClient.infoCmd().exec();
        Map<String, RuntimeInfo> runtimes = info.getRuntimes();
        if (runtimes != null) {
            if (runtimes.containsKey("nvidia")) {
                withCreateContainerCmdModifier(cmd -> {
                    cmd
                        .getHostConfig()
                        .withDeviceRequests(
                            Collections.singletonList(
                                new DeviceRequest()
                                    .withCapabilities(Collections.singletonList(Collections.singletonList("gpu")))
                                    .withCount(-1)
                            )
                        );
                });
            }
        }
        withExposedPorts(8000);
        withStartupTimeout(Duration.ofSeconds(360));
    }

    public NimContainer withNgcApiKey(String apiKey) {
        withEnv("NGC_API_KEY", apiKey);
        return this;
    }

    public String getEndpoint() {
        return "http://" + getHost() + ":" + getMappedPort(8000);
    }
}

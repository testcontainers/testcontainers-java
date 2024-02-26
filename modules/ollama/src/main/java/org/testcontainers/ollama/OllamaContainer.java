package org.testcontainers.ollama;

import com.github.dockerjava.api.model.DeviceRequest;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.RuntimeInfo;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.Map;

public class OllamaContainer extends GenericContainer<OllamaContainer> {

    private static final DockerImageName DOCKER_IMAGE_NAME = DockerImageName.parse("ollama/ollama");

    public OllamaContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public OllamaContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DOCKER_IMAGE_NAME);

        Info info = this.dockerClient.infoCmd().exec();
        Map<String, RuntimeInfo> runtimes = info.getRuntimes();
        if (runtimes != null) {
            if (runtimes.containsKey("nvidia")) {
                withCreateContainerCmdModifier(cmd -> {
                    cmd
                        .getHostConfig()
                        .withDeviceRequests(
                            Arrays.asList(
                                new DeviceRequest().withCapabilities(Arrays.asList(Arrays.asList("gpu"))).withCount(-1)
                            )
                        );
                });
            }
        }
        withExposedPorts(11434);
    }

    public String getEndpoint() {
        return "http://" + getHost() + ":" + getMappedPort(11434);
    }
}

package org.testcontainers.ollama;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.DeviceRequest;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.RuntimeInfo;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Testcontainers implementation for Ollama.
 * <p>
 * Supported image: {@code ollama/ollama}
 * <p>
 * Exposed ports: 11434
 */
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
                            Collections.singletonList(
                                new DeviceRequest()
                                    .withCapabilities(Collections.singletonList(Collections.singletonList("gpu")))
                                    .withCount(-1)
                            )
                        );
                });
            }
        }
        withExposedPorts(11434);
    }

    /**
     * Create a new image from the container
     * @param imageName the name of the new image
     */
    public void createImage(String imageName) {
        DockerImageName dockerImageName = DockerImageName.parse(getDockerImageName());
        if (!dockerImageName.equals(DockerImageName.parse(imageName))) {
            DockerClient dockerClient = DockerClientFactory.instance().client();
            List<Image> images = dockerClient.listImagesCmd().withReferenceFilter(imageName).exec();
            if (images.isEmpty()) {
                DockerImageName imageModel = DockerImageName.parse(imageName);
                dockerClient
                    .commitCmd(getContainerId())
                    .withRepository(imageModel.getUnversionedPart())
                    .withLabels(Collections.singletonMap("org.testcontainers.sessionId", ""))
                    .withTag(imageModel.getVersionPart())
                    .exec();
            }
        }
    }

    public String getEndpoint() {
        return "http://" + getHost() + ":" + getMappedPort(11434);
    }
}

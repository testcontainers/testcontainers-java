package org.testcontainers.ollama;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.DeviceRequest;
import com.github.dockerjava.api.model.Image;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.List;

/**
 * Testcontainers implementation for Ollama.
 * <p>
 * Supported image: {@code ollama/ollama}
 * <p>
 * Exposed ports: 11434
 * <p>
 * GPU access is opt-in: use {@link #withGpuAccess()} to request access to the host GPU(s).
 */
public class OllamaContainer extends GenericContainer<OllamaContainer> {

    private static final DockerImageName DOCKER_IMAGE_NAME = DockerImageName.parse("ollama/ollama");

    private static final int OLLAMA_PORT = 11434;

    public OllamaContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public OllamaContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DOCKER_IMAGE_NAME);

        withExposedPorts(OLLAMA_PORT);
    }

    /**
     * Requests access to the host GPU(s) by adding a {@link DeviceRequest} to the container's create command.
     * <p>
     * GPU access is opt-in and not enabled automatically: the presence of an {@code nvidia} runtime does not
     * guarantee that a usable GPU is present. Notably, Docker Desktop (WSL2) registers such a runtime even on
     * machines without NVIDIA GPUs, which would make the container fail to start if a GPU device request was
     * applied automatically.
     *
     * @return this
     */
    public OllamaContainer withGpuAccess() {
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
        return self();
    }

    /**
     * Commits the current file system changes in the container into a new image.
     * Should be used for creating an image that contains a loaded model.
     * @param imageName the name of the new image
     */
    public void commitToImage(String imageName) {
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

    public int getPort() {
        return getMappedPort(OLLAMA_PORT);
    }

    public String getEndpoint() {
        return "http://" + getHost() + ":" + getPort();
    }
}

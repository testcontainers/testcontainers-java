package org.testcontainers.ollama;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.DeviceRequest;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.RuntimeInfo;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
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
@Slf4j
public class OllamaContainer extends GenericContainer<OllamaContainer> {

    private static final String FULL_IMAGE_NAME = "ollama/ollama";

    private static final DockerImageName DOCKER_IMAGE_NAME = DockerImageName.parse(FULL_IMAGE_NAME);

    private HuggingFaceModel huggingFaceModel;

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

    public String getEndpoint() {
        return "http://" + getHost() + ":" + getMappedPort(11434);
    }

    public OllamaContainer withHuggingFaceModel(HuggingFaceModel model) {
        this.huggingFaceModel = model;
        return this;
    }

    @Override
    protected void configure() {
        super.configure();

        if (huggingFaceModel != null) {
            this.setImage(new ImageFromDockerfile()
                .withDockerfileFromBuilder(builder -> {
                    builder
                        .from(this.getDockerImageName())
                        .run("apt-get update && apt-get upgrade -y && apt-get install -y python3-pip")
                        .run("pip install huggingface-hub")
                        .build();
                })
            );
        }

    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);

        if (huggingFaceModel == null) {
            return;
        }

        try {
            ExecResult downloadModelFromHF = execInContainer("huggingface-cli", "download",
                huggingFaceModel.repository,
                huggingFaceModel.model,
                "--local-dir", ".");
            if (downloadModelFromHF.getExitCode() > 0) {
                throw new ContainerLaunchException("Failed to download model: " + downloadModelFromHF.getStderr());
            }
            ExecResult createModelFile = execInContainer("touch", "Modelfile");
            if (createModelFile.getExitCode() > 0) {
                throw new ContainerLaunchException("Failed to create Modelfile: " + createModelFile.getStderr());
            }
            ExecResult fillModelFile = execInContainer("sh", "-c", String.format("echo '%s' > Modelfile", huggingFaceModel.modelfile));
            if (fillModelFile.getExitCode() > 0) {
                throw new ContainerLaunchException("Failed to fill Modelfile: " + fillModelFile.getStderr());
            }

            ExecResult buildModel = execInContainer("ollama", "create", huggingFaceModel.name, "-f", "Modelfile");
            if (buildModel.getExitCode() > 0) {
                throw new ContainerLaunchException("Failed to build model: " + buildModel.getStderr());
            }
        } catch (IOException | InterruptedException e) {
            throw new ContainerLaunchException(e.getMessage());
        }

    }

    public static class HuggingFaceModel {
        public final String repository;

        public final String model;

        public String modelfile;

        public String name;

        public HuggingFaceModel(String repository, String model) {
            this.repository = repository;
            this.model = model;
            this.modelfile = "FROM " + model;
            this.name = model.split("\\.")[0];
        }

        public HuggingFaceModel withModelfile(String modelfile) {
            this.modelfile = modelfile;
            return this;
        }

        public HuggingFaceModel withName(String name) {
            this.name = name;
            return this;
        }

    }
}

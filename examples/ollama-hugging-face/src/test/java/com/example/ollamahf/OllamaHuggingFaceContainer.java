package com.example.ollamahf;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.ollama.OllamaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

public class OllamaHuggingFaceContainer extends OllamaContainer {

    private final String imageName;

    private final HuggingFaceModel huggingFaceModel;

    public OllamaHuggingFaceContainer(String imageName, HuggingFaceModel model) {
        super(DockerImageName.parse("ollama/ollama:0.1.42"));
        this.imageName = imageName;
        this.huggingFaceModel = model;
    }

    @Override
    protected void configure() {
        super.configure();

        if (huggingFaceModel != null) {
            this.setImage(
                    new ImageFromDockerfile()
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
    protected void containerIsStarted(InspectContainerResponse containerInfo, boolean reused) {
        super.containerIsStarted(containerInfo, reused);
        if (reused) {
            return;
        }

        if (huggingFaceModel == null) {
            return;
        }

        try {
            ExecResult downloadModelFromHF = execInContainer(
                "huggingface-cli",
                "download",
                huggingFaceModel.repository,
                huggingFaceModel.model,
                "--local-dir",
                "."
            );
            if (downloadModelFromHF.getExitCode() > 0) {
                throw new ContainerLaunchException("Failed to download model: " + downloadModelFromHF.getStderr());
            }

            ExecResult fillModelFile = execInContainer(
                "sh",
                "-c",
                String.format("echo '%s' > Modelfile", huggingFaceModel.modelfileContent)
            );
            if (fillModelFile.getExitCode() > 0) {
                throw new ContainerLaunchException("Failed to fill Modelfile: " + fillModelFile.getStderr());
            }

            ExecResult buildModel = execInContainer("ollama", "create", huggingFaceModel.model, "-f", "Modelfile");
            if (buildModel.getExitCode() > 0) {
                throw new ContainerLaunchException("Failed to build model: " + buildModel.getStderr());
            }
        } catch (IOException | InterruptedException e) {
            throw new ContainerLaunchException(e.getMessage());
        }

        commitToImage(imageName);
    }

    public static class HuggingFaceModel {

        public final String repository;

        public final String model;

        public String modelfileContent;

        public HuggingFaceModel(String repository, String model) {
            this.repository = repository;
            this.model = model;
            this.modelfileContent = "FROM " + model;
        }
    }
}

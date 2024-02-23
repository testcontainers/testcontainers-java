package org.testcontainers.ollama;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class OllamaContainer extends GenericContainer<OllamaContainer> {

    private static final DockerImageName DOCKER_IMAGE_NAME = DockerImageName.parse("ollama/ollama");

    public OllamaContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public OllamaContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DOCKER_IMAGE_NAME);

        withExposedPorts(11434);
    }

    public String getEndpoint() {
        return "http://" + getHost() + ":" + getMappedPort(11434);
    }
}

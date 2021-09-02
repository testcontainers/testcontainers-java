package org.testcontainers.images.builder;

import org.junit.Test;
import org.testcontainers.ContainerControllerFactory;
import org.testcontainers.controller.ContainerController;
import org.testcontainers.controller.UnsupportedProviderOperationException;
import org.testcontainers.controller.intents.InspectImageResult;
import org.testcontainers.docker.DockerClientFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class ImageFromDockerfileTest {

    @Test
    public void shouldAddDefaultLabels() throws UnsupportedProviderOperationException {
        ImageFromDockerfile image = new ImageFromDockerfile()
            .withDockerfileFromBuilder(it -> it.from("scratch"));

        String imageId = image.resolve();

        ContainerController dockerClient = ContainerControllerFactory.instance().controller();

        InspectImageResult inspectImageResponse = dockerClient.inspectImageIntent(imageId).perform();

        assertThat(inspectImageResponse.getConfig().getLabels())
            .containsAllEntriesOf(DockerClientFactory.DEFAULT_LABELS); // TODO: Implement!
    }

}

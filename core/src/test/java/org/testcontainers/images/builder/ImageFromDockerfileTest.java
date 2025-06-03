package org.testcontainers.images.builder;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.Base58;

import static org.assertj.core.api.Assertions.assertThat;

public class ImageFromDockerfileTest {

    @Test
    public void shouldAddDefaultLabels() {
        ImageFromDockerfile image = new ImageFromDockerfile().withDockerfileFromBuilder(it -> it.from("scratch"));

        String imageId = image.resolve();

        DockerClient dockerClient = DockerClientFactory.instance().client();

        InspectImageResponse inspectImageResponse = dockerClient.inspectImageCmd(imageId).exec();

        assertThat(inspectImageResponse.getConfig().getLabels())
            .containsAllEntriesOf(DockerClientFactory.DEFAULT_LABELS);
    }

    @Test
    public void shouldNotAddSessionLabelIfDeleteOnExitIsFalse() {
        ImageFromDockerfile image = new ImageFromDockerfile(
            "localhost/testcontainers/" + Base58.randomString(16).toLowerCase(),
            false
        )
            .withDockerfileFromBuilder(it -> it.from("scratch"));
        String imageId = image.resolve();

        DockerClient dockerClient = DockerClientFactory.instance().client();

        try {
            InspectImageResponse inspectImageResponse = dockerClient.inspectImageCmd(imageId).exec();
            assertThat(inspectImageResponse.getConfig().getLabels())
                .doesNotContainKey(DockerClientFactory.TESTCONTAINERS_SESSION_ID_LABEL);
        } finally {
            // ensure the image is deleted, even if the test fails
            dockerClient.removeImageCmd(imageId).exec();
        }
    }
}

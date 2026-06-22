package org.testcontainers.images.builder;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectImageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.Base58;

import java.io.File;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageFromDockerfileTest {

    @Test
    void shouldAddDefaultLabels() {
        ImageFromDockerfile image = new ImageFromDockerfile().withDockerfileFromBuilder(it -> it.from("scratch"));

        String imageId = image.resolve();

        DockerClient dockerClient = DockerClientFactory.instance().client();

        InspectImageResponse inspectImageResponse = dockerClient.inspectImageCmd(imageId).exec();

        assertThat(inspectImageResponse.getConfig().getLabels())
            .containsAllEntriesOf(DockerClientFactory.DEFAULT_LABELS);
    }

    @Test
    void shouldNotAddSessionLabelIfDeleteOnExitIsFalse() {
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

    @ParameterizedTest
    @ValueSource(
        strings = {
            "..",
            ".",
            "../",
            "./",
            "xxx/",
            "yyy/xxx/",
            "/xxx/",
            "/yyy/xxx/",
            "/..",
            "/.",
            "/../",
            "/./",
            ".",
            "..",
            "aa/.",
            "aa/..",
            "bb/./",
            "bb/../",
            "cc./",
            "cc../",
        }
    )
    void unableToTransferFileWithDotsToDockerDaemon(String tarPath) {
        assertThatThrownBy(() -> {
                new ImageFromDockerfile()
                    .withFileFromFile(tarPath, new File("src/test/resources/mappable-resource/test-resource.txt"));
            })
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(
                "Unable to store file '" +
                Paths.get("src", "test", "resources", "mappable-resource", "test-resource.txt") +
                "' to docker path '" +
                tarPath +
                "'"
            );
    }
}

package org.testcontainers.junit;

import com.github.dockerjava.api.model.Container;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DockerComposeContainerWithBuildTest {

    public static Stream<Arguments> provideParameters() {
        return Stream.of(
            Arguments.of(null, true, true),
            Arguments.of(DockerComposeContainer.RemoveImages.LOCAL, false, true),
            Arguments.of(DockerComposeContainer.RemoveImages.ALL, false, false)
        );
    }

    @ParameterizedTest(name = "removeMode = {0}")
    @MethodSource("provideParameters")
    public void performTest(
        DockerComposeContainer.RemoveImages removeMode,
        boolean shouldBuiltImageBePresentAfterRunning,
        boolean shouldPulledImageBePresentAfterRunning
    ) {
        final File composeFile = new File("src/test/resources/compose-build-test/docker-compose.yml");

        final AtomicReference<String> builtImageName = new AtomicReference<>("");
        final AtomicReference<String> pulledImageName = new AtomicReference<>("");
        try (
            DockerComposeContainer environment = new DockerComposeContainer<>(composeFile)
                .withExposedService("customredis", 6379)
                .withBuild(true)
                .withRemoveImages(removeMode)
        ) {
            environment.start();

            builtImageName.set(imageNameForRunningContainer("_customredis_1"));
            final boolean isBuiltImagePresentWhileRunning = isImagePresent(builtImageName.get());
            assertThat(isBuiltImagePresentWhileRunning).as("the built image is present while running").isEqualTo(true);

            pulledImageName.set(imageNameForRunningContainer("_normalredis_1"));
            final boolean isPulledImagePresentWhileRunning = isImagePresent(pulledImageName.get());
            assertThat(isPulledImagePresentWhileRunning)
                .as("the pulled image is present while running")
                .isEqualTo(true);
        }

        Unreliables.retryUntilSuccess(
            10,
            TimeUnit.SECONDS,
            () -> {
                final boolean isBuiltImagePresentAfterRunning = isImagePresent(builtImageName.get());
                assertThat(isBuiltImagePresentAfterRunning)
                    .as("the built image is not present after running")
                    .isEqualTo(shouldBuiltImageBePresentAfterRunning);
                return null;
            }
        );

        Unreliables.retryUntilSuccess(
            10,
            TimeUnit.SECONDS,
            () -> {
                final boolean isPulledImagePresentAfterRunning = isImagePresent(pulledImageName.get());
                assertThat(isPulledImagePresentAfterRunning)
                    .as("the pulled image is present after running")
                    .isEqualTo(shouldPulledImageBePresentAfterRunning);
                return null;
            }
        );
    }

    private String imageNameForRunningContainer(final String containerNameSuffix) {
        return DockerClientFactory
            .instance()
            .client()
            .listContainersCmd()
            .exec()
            .stream()
            .filter(it -> Stream.of(it.getNames()).anyMatch(name -> name.endsWith(containerNameSuffix)))
            .findFirst()
            .map(Container::getImage)
            .orElseThrow(IllegalStateException::new);
    }

    private boolean isImagePresent(final String imageName) {
        return DockerClientFactory
            .instance()
            .client()
            .listImagesCmd()
            .withImageNameFilter(imageName)
            .exec()
            .stream()
            .findFirst()
            .isPresent();
    }
}

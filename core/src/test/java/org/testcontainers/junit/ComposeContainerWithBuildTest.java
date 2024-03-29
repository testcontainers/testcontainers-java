package org.testcontainers.junit;

import com.github.dockerjava.api.model.Container;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ComposeContainer;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ComposeContainerWithBuildTest {

    public ComposeContainerWithBuildTest(
        final ComposeContainer.RemoveImages removeMode,
        final boolean shouldBuiltImageBePresentAfterRunning,
        final boolean shouldPulledImageBePresentAfterRunning
    ) {
        this.removeMode = removeMode;
        this.shouldBuiltImageBePresentAfterRunning = shouldBuiltImageBePresentAfterRunning;
        this.shouldPulledImageBePresentAfterRunning = shouldPulledImageBePresentAfterRunning;
    }

    public final ComposeContainer.RemoveImages removeMode;

    public final boolean shouldBuiltImageBePresentAfterRunning;

    public final boolean shouldPulledImageBePresentAfterRunning;

    @Parameterized.Parameters(name = "removeMode = {0}")
    public static Object[][] params() {
        return new Object[][] {
            { null, true, true },
            { ComposeContainer.RemoveImages.LOCAL, false, true },
            { ComposeContainer.RemoveImages.ALL, false, false },
        };
    }

    @Test
    public void performTest() {
        final File composeFile = new File("src/test/resources/compose-v2-build-test/docker-compose.yml");

        final AtomicReference<String> builtImageName = new AtomicReference<>("");
        final AtomicReference<String> pulledImageName = new AtomicReference<>("");
        try (
            ComposeContainer environment = new ComposeContainer(composeFile)
                .withExposedService("customredis", 6379)
                .withBuild(true)
                .withRemoveImages(removeMode)
        ) {
            environment.start();

            builtImageName.set(imageNameForRunningContainer("-customredis-1"));
            final boolean isBuiltImagePresentWhileRunning = isImagePresent(builtImageName.get());
            assertThat(isBuiltImagePresentWhileRunning).as("the built image is present while running").isTrue();

            pulledImageName.set(imageNameForRunningContainer("-normalredis-1"));
            final boolean isPulledImagePresentWhileRunning = isImagePresent(pulledImageName.get());
            assertThat(isPulledImagePresentWhileRunning).as("the pulled image is present while running").isTrue();
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

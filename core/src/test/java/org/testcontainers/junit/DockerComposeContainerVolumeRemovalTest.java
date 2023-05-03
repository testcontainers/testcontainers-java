package org.testcontainers.junit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.DockerComposeContainer;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class DockerComposeContainerVolumeRemovalTest {

    public DockerComposeContainerVolumeRemovalTest(
        final boolean removeVolumes,
        final boolean shouldVolumesBePresentAfterRunning
    ) {
        this.removeVolumes = removeVolumes;
        this.shouldVolumesBePresentAfterRunning = shouldVolumesBePresentAfterRunning;
    }

    public final boolean removeVolumes;

    public final boolean shouldVolumesBePresentAfterRunning;

    @Parameterized.Parameters(name = "removeVolumes = {0}")
    public static Object[][] params() {
        return new Object[][] { { true, false }, { false, true } };
    }

    @Test
    public void performTest() {
        final File composeFile = new File("src/test/resources/compose-test.yml");

        final AtomicReference<String> volumeName = new AtomicReference<>("");
        try (
            DockerComposeContainer environment = new DockerComposeContainer<>(composeFile)
                .withExposedService("redis", 6379)
                .withRemoveVolumes(removeVolumes)
                .withRemoveImages(DockerComposeContainer.RemoveImages.ALL)
        ) {
            environment.start();

            volumeName.set(volumeNameForRunningContainer("_redis_1"));
            final boolean isVolumePresentWhileRunning = isVolumePresent(volumeName.get());
            assertThat(isVolumePresentWhileRunning).as("the container volume is present while running").isEqualTo(true);
        }

        Unreliables.retryUntilSuccess(
            10,
            TimeUnit.SECONDS,
            () -> {
                final boolean isVolumePresentAfterRunning = isVolumePresent(volumeName.get());
                assertThat(isVolumePresentAfterRunning)
                    .as("the container volume is present after running")
                    .isEqualTo(shouldVolumesBePresentAfterRunning);
                return null;
            }
        );
    }

    private String volumeNameForRunningContainer(final String containerNameSuffix) {
        return DockerClientFactory
            .instance()
            .client()
            .listContainersCmd()
            .exec()
            .stream()
            .filter(it -> Stream.of(it.getNames()).anyMatch(name -> name.endsWith(containerNameSuffix)))
            .findFirst()
            .map(container -> container.getMounts().get(0).getName())
            .orElseThrow(IllegalStateException::new);
    }

    private boolean isVolumePresent(final String volumeName) {
        LinkedHashSet<String> nameFilter = new LinkedHashSet<>(1);
        nameFilter.add(volumeName);
        return DockerClientFactory
            .instance()
            .client()
            .listVolumesCmd()
            .withFilter("name", nameFilter)
            .exec()
            .getVolumes()
            .stream()
            .findFirst()
            .isPresent();
    }
}

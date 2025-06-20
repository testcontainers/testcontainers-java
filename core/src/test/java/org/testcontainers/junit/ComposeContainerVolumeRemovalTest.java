package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ComposeContainer;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@ParameterizedClass(name = "removeVolumes = {0}")
@MethodSource("params")
public class ComposeContainerVolumeRemovalTest {

    public ComposeContainerVolumeRemovalTest(
        final boolean removeVolumes,
        final boolean shouldVolumesBePresentAfterRunning
    ) {
        this.removeVolumes = removeVolumes;
        this.shouldVolumesBePresentAfterRunning = shouldVolumesBePresentAfterRunning;
    }

    public final boolean removeVolumes;

    public final boolean shouldVolumesBePresentAfterRunning;

    public static Object[][] params() {
        return new Object[][] { { true, false }, { false, true } };
    }

    @Test
    public void performTest() {
        final File composeFile = new File("src/test/resources/v2-compose-test.yml");

        final AtomicReference<String> volumeName = new AtomicReference<>("");
        try (
            ComposeContainer environment = new ComposeContainer(composeFile)
                .withExposedService("redis", 6379)
                .withRemoveVolumes(this.removeVolumes)
                .withRemoveImages(ComposeContainer.RemoveImages.ALL)
        ) {
            environment.start();

            volumeName.set(volumeNameForRunningContainer("-redis-1"));
            final boolean isVolumePresentWhileRunning = isVolumePresent(volumeName.get());
            assertThat(isVolumePresentWhileRunning).as("the container volume is present while running").isEqualTo(true);
        }

        await()
            .untilAsserted(() -> {
                final boolean isVolumePresentAfterRunning = isVolumePresent(volumeName.get());
                assertThat(isVolumePresentAfterRunning)
                    .as("the container volume is present after running")
                    .isEqualTo(this.shouldVolumesBePresentAfterRunning);
            });
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
        Set<String> nameFilter = new LinkedHashSet<>(1);
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

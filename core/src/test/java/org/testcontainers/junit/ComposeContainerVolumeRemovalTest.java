package org.testcontainers.junit;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
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

public class ComposeContainerVolumeRemovalTest {

    public static Stream<Arguments> provideParameters() {
        return Stream.of(Arguments.of(true, false), Arguments.of(false, true));
    }

    @ParameterizedTest(name = "removeVolumes = {0}")
    @MethodSource("provideParameters")
    public void performTest(boolean removeVolumes, boolean shouldVolumesBePresentAfterRunning) {
        final File composeFile = new File("src/test/resources/v2-compose-test.yml");

        final AtomicReference<String> volumeName = new AtomicReference<>("");
        try (
            ComposeContainer environment = new ComposeContainer(composeFile)
                .withExposedService("redis", 6379)
                .withRemoveVolumes(removeVolumes)
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
                    .isEqualTo(shouldVolumesBePresentAfterRunning);
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

package org.testcontainers.junit;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class DockerComposeContainerVolumeRemovalTest {

    public static Object[][] params() {
        return new Object[][] { { true, false }, { false, true } };
    }

    @ParameterizedTest
    @MethodSource("params")
    void performTest(boolean removeVolumes, boolean shouldVolumesBePresentAfterRunning) {
        final File composeFile = new File("src/test/resources/compose-test.yml");

        final AtomicReference<String> volumeName = new AtomicReference<>("");
        try (
            DockerComposeContainer environment = new DockerComposeContainer<>(
                DockerImageName.parse("docker/compose:1.29.2"),
                composeFile
            )
                .withExposedService("redis", 6379)
                .withRemoveVolumes(removeVolumes)
                .withRemoveImages(DockerComposeContainer.RemoveImages.ALL)
        ) {
            environment.start();

            volumeName.set(volumeNameForRunningContainer("_redis_1"));
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

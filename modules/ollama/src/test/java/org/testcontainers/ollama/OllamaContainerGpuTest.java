package org.testcontainers.ollama;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.DeviceRequest;
import com.github.dockerjava.core.command.CreateContainerCmdImpl;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.core.CreateContainerCmdModifier;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the opt-in GPU access of {@link OllamaContainer}. The tests do not start any container: they
 * apply the registered {@link CreateContainerCmdModifier}s to a {@link CreateContainerCmd} instance and assert
 * on the resulting HostConfig.
 */
class OllamaContainerGpuTest {

    @Test
    void shouldNotRequestGpuAccessByDefault() throws Exception {
        try (OllamaContainer container = new OllamaContainer(DockerImageName.parse("ollama/ollama"))) {
            CreateContainerCmd createCommand = applyCreateContainerCmdModifiers(container);

            assertThat(createCommand.getHostConfig().getDeviceRequests()).isNull();
        }
    }

    @Test
    void shouldRequestGpuAccessWhenOptedIn() throws Exception {
        try (OllamaContainer container = new OllamaContainer(DockerImageName.parse("ollama/ollama"))) {
            OllamaContainer result = container.withGpuAccess();

            assertThat(result).isSameAs(container);

            CreateContainerCmd createCommand = applyCreateContainerCmdModifiers(container);
            DeviceRequest deviceRequest = createCommand.getHostConfig().getDeviceRequests().get(0);

            assertThat(createCommand.getHostConfig().getDeviceRequests()).hasSize(1);
            assertThat(deviceRequest.getCapabilities()).containsExactly(Collections.singletonList("gpu"));
            assertThat(deviceRequest.getCount()).isEqualTo(-1);
        }
    }

    @SuppressWarnings("unchecked")
    private static CreateContainerCmd applyCreateContainerCmdModifiers(OllamaContainer container) throws Exception {
        CreateContainerCmd createCommand = new CreateContainerCmdImpl(
            command -> null, // the command is never executed in this test
            null,
            "ollama/ollama"
        );

        Field modifiersField = GenericContainer.class.getDeclaredField("createContainerCmdModifiers");
        modifiersField.setAccessible(true);
        Set<CreateContainerCmdModifier> modifiers = (Set<CreateContainerCmdModifier>) modifiersField.get(container);
        for (CreateContainerCmdModifier modifier : modifiers) {
            createCommand = modifier.modify(createCommand);
        }
        return createCommand;
    }
}

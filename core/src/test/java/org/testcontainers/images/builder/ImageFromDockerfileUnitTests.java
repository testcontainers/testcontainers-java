package org.testcontainers.images.builder;

import com.github.dockerjava.api.command.BuildImageCmd;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.utility.MockTestcontainersConfigurationExtension;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockTestcontainersConfigurationExtension.class)
class ImageFromDockerfileUnitTests {

    @Test
    void shouldDisableLayerCachingWhenConfigured() {
        doReturn(true).when(TestcontainersConfiguration.getInstance()).isImageFromDockerfileNoCache();

        BuildImageCmd buildImageCmd = mock(BuildImageCmd.class);

        new ImageFromDockerfile("localhost/testcontainers/test-image").configure(buildImageCmd);

        verify(buildImageCmd).withNoCache(true);
    }

    @Test
    void shouldAllowBuildImageCmdModifierToOverrideConfiguredLayerCaching() {
        doReturn(true).when(TestcontainersConfiguration.getInstance()).isImageFromDockerfileNoCache();

        BuildImageCmd buildImageCmd = mock(BuildImageCmd.class);

        new ImageFromDockerfile("localhost/testcontainers/test-image")
            .withBuildImageCmdModifier(command -> command.withNoCache(false))
            .configure(buildImageCmd);

        var inOrder = inOrder(buildImageCmd);
        inOrder.verify(buildImageCmd).withNoCache(true);
        inOrder.verify(buildImageCmd).withNoCache(false);
    }
}

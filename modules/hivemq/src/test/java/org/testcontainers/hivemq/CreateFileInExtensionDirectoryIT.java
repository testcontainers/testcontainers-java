package org.testcontainers.hivemq;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.hivemq.util.TestPublishModifiedUtil;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

class CreateFileInExtensionDirectoryIT {

    @ParameterizedTest
    @ValueSource(
        strings = {
            "2020.1", // first version that provided a container image
            "2024.3", // version that runs the image as a non-root user by default
        }
    )
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test(final @NotNull String hivemqCeTag) throws Exception {
        final HiveMQExtension hiveMQExtension = HiveMQExtension
            .builder()
            .id("extension-1")
            .name("my-extension")
            .version("1.0")
            .mainClass(FileCreatorExtension.class)
            .build();

        try (
            final HiveMQContainer hivemq = new HiveMQContainer(
                DockerImageName.parse("hivemq/hivemq-ce").withTag(hivemqCeTag)
            )
                .withHiveMQConfig(MountableFile.forClasspathResource("/inMemoryConfig.xml"))
                .waitForExtension(hiveMQExtension)
                .withExtension(hiveMQExtension)
        ) {
            hivemq.start();
            TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort(), hivemq.getHost());
        }
    }

    public static class FileCreatorExtension implements ExtensionMain {

        @Override
        public void extensionStart(
            @NotNull ExtensionStartInput extensionStartInput,
            @NotNull ExtensionStartOutput extensionStartOutput
        ) {
            final PublishInboundInterceptor publishInboundInterceptor = (publishInboundInput, publishInboundOutput) -> {
                final File extensionHomeFolder = extensionStartInput.getExtensionInformation().getExtensionHomeFolder();

                final File file = new File(extensionHomeFolder, "myfile.txt");
                try {
                    if (file.createNewFile()) {
                        publishInboundOutput
                            .getPublishPacket()
                            .setPayload(ByteBuffer.wrap("modified".getBytes(StandardCharsets.UTF_8)));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            };

            final ClientInitializer clientInitializer = (initializerInput, clientContext) -> {
                clientContext.addPublishInboundInterceptor(publishInboundInterceptor);
            };

            Services.initializerRegistry().setClientInitializer(clientInitializer);
        }

        @Override
        public void extensionStop(
            @NotNull ExtensionStopInput extensionStopInput,
            @NotNull ExtensionStopOutput extensionStopOutput
        ) {}
    }
}

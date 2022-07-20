package org.testcontainers.hivemq.util;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

@SuppressWarnings("CodeBlock2Expr")
public class MyExtension implements ExtensionMain {

    @Override
    public void extensionStart(
        final @NotNull ExtensionStartInput extensionStartInput,
        final @NotNull ExtensionStartOutput extensionStartOutput
    ) {
        final PublishInboundInterceptor publishInboundInterceptor = (publishInboundInput, publishInboundOutput) -> {
            publishInboundOutput
                .getPublishPacket()
                .setPayload(ByteBuffer.wrap("modified".getBytes(StandardCharsets.UTF_8)));
        };

        final ClientInitializer clientInitializer = (initializerInput, clientContext) -> {
            clientContext.addPublishInboundInterceptor(publishInboundInterceptor);
        };

        Services.initializerRegistry().setClientInitializer(clientInitializer);
    }

    @Override
    public void extensionStop(
        final @NotNull ExtensionStopInput extensionStopInput,
        final @NotNull ExtensionStopOutput extensionStopOutput
    ) {}
}

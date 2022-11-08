package org.testcontainers.hivemq.util;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.intializer.ClientInitializer;
import org.jetbrains.annotations.NotNull;

public class MyExtensionWithSubclasses implements ExtensionMain {

    @Override
    public void extensionStart(
        final @NotNull ExtensionStartInput extensionStartInput,
        final @NotNull ExtensionStartOutput extensionStartOutput
    ) {
        final ClientInitializer clientInitializer = (initializerInput, clientContext) -> {
            clientContext.addPublishInboundInterceptor(new PublishModifier());
        };

        Services.initializerRegistry().setClientInitializer(clientInitializer);
    }

    @Override
    public void extensionStop(
        final @NotNull ExtensionStopInput extensionStopInput,
        final @NotNull ExtensionStopOutput extensionStopOutput
    ) {}
}

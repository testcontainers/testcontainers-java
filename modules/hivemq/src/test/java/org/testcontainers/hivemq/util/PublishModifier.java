package org.testcontainers.hivemq.util;

import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class PublishModifier implements PublishInboundInterceptor {

    @Override
    public void onInboundPublish(
        final @NotNull PublishInboundInput publishInboundInput,
        final @NotNull PublishInboundOutput publishInboundOutput
    ) {
        publishInboundOutput
            .getPublishPacket()
            .setPayload(ByteBuffer.wrap("modified".getBytes(StandardCharsets.UTF_8)));
    }
}

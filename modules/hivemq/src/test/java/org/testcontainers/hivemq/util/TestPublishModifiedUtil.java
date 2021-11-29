/*
 * MIT License
 *
 * Copyright (c) 2021-present HiveMQ GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.testcontainers.hivemq.util;

import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * @author Yannick Weber
 */
public class TestPublishModifiedUtil {

    public static void testPublishModified(final int mqttPort) throws InterruptedException, ExecutionException {
        final CompletableFuture<Void> publishReceived = new CompletableFuture<>();

        final Mqtt5BlockingClient publisher = Mqtt5Client.builder()
                .serverPort(mqttPort)
                .identifier("publisher")
                .buildBlocking();
        publisher.connect();

        final Mqtt5BlockingClient subscriber = Mqtt5Client.builder()
                .serverPort(mqttPort)
                .identifier("subscriber")
                .buildBlocking();
        subscriber.connect();
        subscriber.subscribeWith().topicFilter("test/topic").send();
        subscriber.toAsync().publishes(MqttGlobalPublishFilter.ALL, publish -> {
            if (Arrays.equals(publish.getPayloadAsBytes(), "modified".getBytes(StandardCharsets.UTF_8))) {
                publishReceived.complete(null);
            } else {
                publishReceived.completeExceptionally(new IllegalArgumentException("unexpected payload: " + new String(publish.getPayloadAsBytes())));
            }
        });

        publisher.publishWith().topic("test/topic").payload("unmodified".getBytes(StandardCharsets.UTF_8)).send();

        try {
            publishReceived.get();
        } finally {
            publisher.disconnect();
            subscriber.disconnect();
        }
    }


}

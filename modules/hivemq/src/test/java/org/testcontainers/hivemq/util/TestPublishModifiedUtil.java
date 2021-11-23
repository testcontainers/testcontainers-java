/*
 * Copyright 2020 HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

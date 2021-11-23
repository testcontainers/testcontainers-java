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
package org.testcontainers.hivemq;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.exceptions.MqttSessionExpiredException;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Yannick Weber
 */
public class ContainerWithCustomConfigIT {

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        final HiveMQContainer extension = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("latest"))
                .withHiveMQConfig(MountableFile.forClasspathResource("/config.xml"));

        extension.start();

        final Mqtt5BlockingClient publisher = Mqtt5Client.builder()
                .identifier("publisher")
                .serverPort(extension.getMqttPort())
                .buildBlocking();

        publisher.connect();

        assertThrows(MqttSessionExpiredException.class, () -> {
            // this should fail since only QoS 0 is allowed by the configuration
            publisher.publishWith()
                    .topic("test/topic")
                    .qos(MqttQos.EXACTLY_ONCE)
                    .send();
        });

        extension.stop();
    }
}

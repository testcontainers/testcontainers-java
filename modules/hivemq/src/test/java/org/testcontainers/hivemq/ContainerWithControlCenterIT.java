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

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

/**
 * @author Yannick Weber
 */
public class ContainerWithControlCenterIT {

    public static final int CONTROL_CENTER_PORT = 8080;

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void test() throws Exception {

        final HiveMQContainer extension =
                new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("latest"))
                        .withControlCenter();

        extension.start();

        final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        final HttpUriRequest request = new HttpGet("http://localhost:" + extension.getMappedPort(CONTROL_CENTER_PORT));
        httpClient.execute(request);

        extension.stop();
    }
}

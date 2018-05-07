/*
 * Copyright (c) 2016 Couchbase, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.testcontainers.couchbase;

import com.couchbase.client.deps.com.fasterxml.jackson.databind.JsonNode;
import com.couchbase.client.deps.com.fasterxml.jackson.databind.ObjectMapper;
import org.rnorth.ducttape.TimeoutException;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.shaded.com.google.common.base.Strings;
import org.testcontainers.shaded.com.google.common.io.BaseEncoding;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

/**
 * @author ldoguin
 * created on 18/07/16.
 */
public class CouchbaseWaitStrategy extends GenericContainer.AbstractWaitStrategy {
    /**
     * Authorization HTTP header.
     */
    private static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Basic Authorization scheme prefix.
     */
    private static final String AUTH_BASIC = "Basic ";

    private String path = "/pools/default/";
    private int statusCode = HttpURLConnection.HTTP_OK;
    private boolean tlsEnabled;
    private String username;
    private String password;
    private ObjectMapper om = new ObjectMapper();

    /**
     * Indicates that the status check should use HTTPS.
     *
     * @return this
     */
    public CouchbaseWaitStrategy usingTls() {
        this.tlsEnabled = true;
        return this;
    }

    /**
     * Authenticate with HTTP Basic Authorization credentials.
     *
     * @param username the username
     * @param password the password
     * @return this
     */
    public CouchbaseWaitStrategy withBasicCredentials(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    @Override
    protected void waitUntilReady() {
        final Integer livenessCheckPort = getLivenessCheckPort();
        if (null == livenessCheckPort) {
            logger().warn("No exposed ports or mapped ports - cannot wait for status");
            return;
        }

        final String uri = buildLivenessUri(livenessCheckPort).toString();
        logger().info("Waiting for {} seconds for URL: {}", startupTimeout.getSeconds(), uri);

        // try to connect to the URL
        try {
            retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                getRateLimiter().doWhenReady(() -> {
                    try {
                        final HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();

                        // authenticate
                        if (!Strings.isNullOrEmpty(username)) {
                            connection.setRequestProperty(HEADER_AUTHORIZATION, buildAuthString(username, password));
                            connection.setUseCaches(false);
                        }

                        connection.setRequestMethod("GET");
                        connection.connect();

                        if (statusCode != connection.getResponseCode()) {
                            throw new RuntimeException(String.format("HTTP response code was: %s",
                                    connection.getResponseCode()));
                        }

                        // Specific Couchbase wait strategy to be sure the node is online and healthy
                        JsonNode node = om.readTree(connection.getInputStream());
                        JsonNode statusNode = node.at("/nodes/0/status");
                        String status = statusNode.asText();
                        if (!"healthy".equals(status)){
                            throw new RuntimeException(String.format("Couchbase Node status was: %s", status));
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                return true;
            });

        } catch (TimeoutException e) {
            throw new ContainerLaunchException(String.format(
                    "Timed out waiting for URL to be accessible (%s should return HTTP %s)", uri, statusCode));
        }
    }

    /**
     * Build the URI on which to check if the container is ready.
     *
     * @param livenessCheckPort the liveness port
     * @return the liveness URI
     */
    private URI buildLivenessUri(int livenessCheckPort) {
        final String scheme = (tlsEnabled ? "https" : "http") + "://";
        final String host = container.getContainerIpAddress();

        final String portSuffix;
        if ((tlsEnabled && 443 == livenessCheckPort) || (!tlsEnabled && 80 == livenessCheckPort)) {
            portSuffix = "";
        } else {
            portSuffix = ":" + String.valueOf(livenessCheckPort);
        }

        return URI.create(scheme + host + portSuffix + path);
    }

    /**
     * @param username the username
     * @param password the password
     * @return a basic authentication string for the given credentials
     */
    private String buildAuthString(String username, String password) {
        return AUTH_BASIC + BaseEncoding.base64().encode((username + ":" + password).getBytes());
    }
}